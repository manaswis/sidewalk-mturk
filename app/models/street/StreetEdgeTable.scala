package models.street

import java.sql.Timestamp
import java.util.UUID

import com.vividsolutions.jts.geom.LineString
import models.audit.AuditTaskTable
import models.region.RegionTable
import models.utils.MyPostgresDriver
import models.utils.MyPostgresDriver.simple._
import org.postgresql.util.PSQLException
import play.api.Play.current

import scala.slick.jdbc.{GetResult, StaticQuery => Q}

case class StreetEdge(streetEdgeId: Int, geom: LineString, source: Int, target: Int, x1: Float, y1: Float, x2: Float, y2: Float, wayType: String, deleted: Boolean, timestamp: Option[Timestamp])

/**
 *
 */
class StreetEdgeTable(tag: Tag) extends Table[StreetEdge](tag, Some("sidewalk"), "street_edge") {
  def streetEdgeId = column[Int]("street_edge_id", O.PrimaryKey)
  def geom = column[LineString]("geom")
  def source = column[Int]("source")
  def target = column[Int]("target")
  def x1 = column[Float]("x1")
  def y1 = column[Float]("y1")
  def x2 = column[Float]("x2")
  def y2 = column[Float]("y2")
  def wayType = column[String]("way_type")
  def deleted = column[Boolean]("deleted", O.Default(false))
  def timestamp = column[Option[Timestamp]]("timestamp")

  def * = (streetEdgeId, geom, source, target, x1, y1, x2, y2, wayType, deleted, timestamp) <> ((StreetEdge.apply _).tupled, StreetEdge.unapply)
}


/**
 * Data access object for the street_edge table
 */
object StreetEdgeTable {
  // For plain query
  // https://github.com/tminglei/slick-pg/blob/slick2/src/test/scala/com/github/tminglei/slickpg/addon/PgPostGISSupportTest.scala
  import MyPostgresDriver.plainImplicits._

  implicit val streetEdgeConverter = GetResult[StreetEdge](r => {
    val streetEdgeId = r.nextInt
    val geometry = r.nextGeometry[LineString]
    val source = r.nextInt
    val target = r.nextInt
    val x1 = r.nextFloat
    val y1 = r.nextFloat
    val x2 = r.nextFloat
    val y2 = r.nextFloat
    val wayType = r.nextString
    val deleted = r.nextBoolean
    val timestamp = r.nextTimestampOption
    StreetEdge(streetEdgeId, geometry, source, target, x1, y1, x2, y2, wayType, deleted, timestamp)
  })

  val db = play.api.db.slick.DB
  val auditTasks = TableQuery[AuditTaskTable]
  val regions = TableQuery[RegionTable]
  val streetEdges = TableQuery[StreetEdgeTable]
  val streetEdgeAssignmentCounts = TableQuery[StreetEdgeAssignmentCountTable]
  val streetEdgeRegion = TableQuery[StreetEdgeRegionTable]

  val neighborhoods = regions.filter(_.deleted === false).filter(_.regionTypeId === 2)

  val completedAuditTasks = auditTasks.filter(_.completed === true)
  val streetEdgesWithoutDeleted = streetEdges.filter(_.deleted === false)
  val streetEdgeNeighborhood = for { (se, n) <- streetEdgeRegion.innerJoin(neighborhoods).on(_.regionId === _.regionId) } yield se


  /**
   * Returns a list of all the street edges
    *
    * @return A list of StreetEdge objects.
   */
  def all: List[StreetEdge] = db.withSession { implicit session =>
    streetEdgesWithoutDeleted.list
  }

  /**
    * Count the number of streets that have been audited at least a given number of times
    *
    * @return
    */
  def countTotalStreets(): Int = db.withSession { implicit session =>
    all.size
  }
  
  /**
    * This method returns the audit completion rate
    *
    * @param auditCount
    * @return
    */
  def auditCompletionRate(auditCount: Int): Float = db.withSession { implicit session =>
    val allEdges = streetEdgesWithoutDeleted.list
    countAuditedStreets(auditCount).toFloat / allEdges.length
  }

  /**
    * Get the total distance in miles
    * Reference: http://gis.stackexchange.com/questions/143436/how-do-i-calculate-st-length-in-miles
    *
    * @return
    */
  def totalStreetDistance(): Float = db.withSession { implicit session =>
    // DISTINCT query: http://stackoverflow.com/questions/18256768/select-distinct-in-scala-slick

    val distances: List[Float] = streetEdgesWithoutDeleted.groupBy(x => x).map(_._1.geom.transform(26918).length).list
    (distances.sum * 0.000621371).toFloat
  }

  /**
    * Get the audited distance in miles
    * Reference: http://gis.stackexchange.com/questions/143436/how-do-i-calculate-st-length-in-miles
    *
    * @param auditCount
    * @return
    */
  def auditedStreetDistance(auditCount: Int): Float = db.withSession { implicit session =>
    // DISTINCT query: http://stackoverflow.com/questions/18256768/select-distinct-in-scala-slick
    val edges = for {
      (_streetEdges, _auditTasks) <- streetEdgesWithoutDeleted.innerJoin(completedAuditTasks).on(_.streetEdgeId === _.streetEdgeId)
    } yield _streetEdges
    val distances: List[Float] = edges.groupBy(x => x).map(_._1.geom.transform(26918).length).list
    (distances.sum * 0.000621371).toFloat
  }

  /**
    * Count the number of streets that have been audited at least a given number of times
    *
    * @param auditCount
    * @return
    */
  def countAuditedStreets(auditCount: Int = 1): Int = db.withSession { implicit session =>
    selectAuditedStreets(auditCount).size
  }

  /**
    * Returns a list of street edges that are audited at least auditCount times
    *
    * @return
    */
  def selectAuditedStreets(auditCount: Int = 1): List[StreetEdge] = db.withSession { implicit session =>
    val edges = for {
      (_streetEdges, _auditTasks) <- streetEdgesWithoutDeleted.innerJoin(completedAuditTasks).on(_.streetEdgeId === _.streetEdgeId)
    } yield _streetEdges

    val uniqueStreetEdges: List[StreetEdge] = (for ((eid, groupedEdges) <- edges.list.groupBy(_.streetEdgeId)) yield {
      // Filter out group of edges with the size less than the passed `auditCount`
      if (auditCount > 0 && groupedEdges.size >= auditCount) {
        Some(groupedEdges.head)
      } else {
        None
      }
    }).toList.flatten

    uniqueStreetEdges
  }

  /**
    * Returns all the streets in the given region that has been audited
    * @param regionId
    * @param auditCount
    * @return
    */
  def selectAuditedStreetsByARegionId(regionId: Int, auditCount: Int = 1): List[StreetEdge] = db.withSession { implicit session =>
    val selectAuditedStreetsQuery = Q.query[Int, StreetEdge](
      """SELECT street_edge.street_edge_id, street_edge.geom, source, target, x1, y1, x2, y2, way_type, street_edge.deleted, street_edge.timestamp
        |  FROM sidewalk.street_edge
        |INNER JOIN sidewalk.region
        |  ON ST_Intersects(street_edge.geom, region.geom)
        |INNER JOIN sidewalk.audit_task
        |  ON street_edge.street_edge_id = audit_task.street_edge_id
        |  AND audit_task.completed = TRUE
        |WHERE region.region_id=?
        |  AND street_edge.deleted=FALSE
      """.stripMargin
    )
    selectAuditedStreetsQuery(regionId).list.groupBy(_.streetEdgeId).map(_._2.head).toList
  }

  def selectStreetsAuditedByAUser(userId: UUID, regionId: Int): List[StreetEdge] = db.withSession { implicit session =>
    val selectAuditedStreetsQuery = Q.query[(String, Int), StreetEdge](
      """SELECT street_edge.street_edge_id, street_edge.geom, source, target, x1, y1, x2, y2, way_type, street_edge.deleted, street_edge.timestamp
        |  FROM sidewalk.street_edge
        |INNER JOIN sidewalk.street_edge_region
        |  ON street_edge_region.street_edge_id = street_edge.street_edge_id
        |INNER JOIN sidewalk.audit_task
        |  ON street_edge.street_edge_id = audit_task.street_edge_id
        |  AND audit_task.completed = TRUE
        |  AND audit_task.user_id = ?
        |WHERE street_edge_region.region_id=?
        |  AND street_edge.deleted=FALSE
      """.stripMargin
    )
    selectAuditedStreetsQuery((userId.toString, regionId)).list.groupBy(_.streetEdgeId).map(_._2.head).toList
  }

  /**
    * Returns all the streets intersecting the neighborhood
    * @param regionId
    * @param auditCount
    * @return
    */
  def selectStreetsByARegionId(regionId: Int, auditCount: Int = 1): List[StreetEdge] = db.withSession { implicit session =>
    val selectAuditedStreetsQuery = Q.query[Int, StreetEdge](
      """SELECT street_edge.street_edge_id, street_edge.geom, source, target, x1, y1, x2, y2, way_type, street_edge.deleted, street_edge.timestamp
        |  FROM sidewalk.street_edge
        |INNER JOIN sidewalk.region
        |  ON ST_Intersects(street_edge.geom, region.geom)
        |WHERE region.region_id=?
        |  AND street_edge.deleted=FALSE
      """.stripMargin
    )

    try {
      selectAuditedStreetsQuery(regionId).list
    } catch {
      case e: PSQLException => List()
    }
  }

  def selectStreetsIntersecting(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double): List[StreetEdge] = db.withSession { implicit session =>
    // http://gis.stackexchange.com/questions/60700/postgis-select-by-lat-long-bounding-box
    // http://postgis.net/docs/ST_MakeEnvelope.html
    val selectEdgeQuery = Q.query[(Double, Double, Double, Double), StreetEdge](
      """SELECT st_e.street_edge_id, st_e.geom, st_e.source, st_e.target, st_e.x1, st_e.y1, st_e.x2, st_e.y2, st_e.way_type, st_e.deleted, st_e.timestamp
       |FROM sidewalk.street_edge AS st_e
       |WHERE st_e.deleted = FALSE AND ST_Intersects(st_e.geom, ST_MakeEnvelope(?, ?, ?, ?, 4326))""".stripMargin
    )

    val edges: List[StreetEdge] = selectEdgeQuery((minLng, minLat, maxLng, maxLat)).list
    edges
  }

  def selectAuditedStreetsIntersecting(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double): List[StreetEdge] = db.withSession { implicit session =>
    // http://gis.stackexchange.com/questions/60700/postgis-select-by-lat-long-bounding-box
    // http://postgis.net/docs/ST_MakeEnvelope.html
    val selectEdgeQuery = Q.query[(Double, Double, Double, Double), StreetEdge](
      """SELECT DISTINCT(street_edge.street_edge_id), street_edge.geom, street_edge.source, street_edge.target, street_edge.x1, street_edge.y1, street_edge.x2, street_edge.y2, street_edge.way_type, street_edge.deleted, street_edge.timestamp
        |  FROM sidewalk.street_edge
        |  INNER JOIN sidewalk.audit_task
        |  ON street_edge.street_edge_id = audit_task.street_edge_id
        |  WHERE street_edge.deleted = FALSE
        |  AND ST_Intersects(street_edge.geom, ST_MakeEnvelope(?, ?, ?, ?, 4326))
        |  AND audit_task.completed = TRUE""".stripMargin
    )

    val edges: List[StreetEdge] = selectEdgeQuery((minLng, minLat, maxLng, maxLat)).list
    edges
  }

  def selectStreetsWithin(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double): List[StreetEdge] = db.withSession { implicit session =>
    val selectEdgeQuery = Q.query[(Double, Double, Double, Double), StreetEdge](
      """SELECT DISTINCT(st_e.street_edge_id), st_e.geom, st_e.source, st_e.target, st_e.x1, st_e.y1, st_e.x2, st_e.y2, st_e.way_type, st_e.deleted, st_e.timestamp
        |FROM sidewalk.street_edge AS st_e
        |WHERE st_e.deleted = FALSE
        |AND ST_Within(st_e.geom, ST_MakeEnvelope(?, ?, ?, ?, 4326))""".stripMargin
    )

    val edges: List[StreetEdge] = selectEdgeQuery((minLng, minLat, maxLng, maxLat)).list
    edges
  }

  def selectAuditedStreetsWithin(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double): List[StreetEdge] = db.withSession { implicit session =>
    val selectEdgeQuery = Q.query[(Double, Double, Double, Double), StreetEdge](
      """SELECT DISTINCT(street_edge.street_edge_id), street_edge.geom, street_edge.source, street_edge.target, street_edge.x1, street_edge.y1, street_edge.x2, street_edge.y2, street_edge.way_type, street_edge.deleted, street_edge.timestamp
        |  FROM sidewalk.street_edge
        |  INNER JOIN sidewalk.audit_task
        |  ON street_edge.street_edge_id = audit_task.street_edge_id
        |  WHERE street_edge.deleted = FALSE
        |  AND ST_Within(street_edge.geom, ST_MakeEnvelope(?, ?, ?, ?, 4326))
        |  AND audit_task.completed = TRUE""".stripMargin
    )

    val edges: List[StreetEdge] = selectEdgeQuery((minLng, minLat, maxLng, maxLat)).list
    edges
  }

  /**
   * Set a record's deleted column to true
   */
  def delete(id: Int) = db.withSession { implicit session =>
    streetEdges.filter(edge => edge.streetEdgeId === id).map(_.deleted).update(true)
  }

  /**
   * Save a StreetEdge into the street_edge table
    *
    * @param edge A StreetEdge object
   * @return
   */
  def save(edge: StreetEdge): Int = db.withTransaction { implicit session =>
    streetEdges += edge
    edge.streetEdgeId // return the edge id.
  }
}

