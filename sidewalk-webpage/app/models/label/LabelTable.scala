package models.label

import java.util.UUID

import models.audit.{AuditTask, AuditTaskTable}
import models.utils.MyPostgresDriver.simple._
import play.api.Play.current

import scala.slick.lifted.ForeignKeyQuery

case class Label(labelId: Int, auditTaskId: Int, gsvPanoramaId: String, labelTypeId: Int,
                 photographerHeading: Float, photographerPitch: Float,
                  panoramaLat: Float, panoramaLng: Float, deleted: Boolean)
case class LabelLocation(labelId: Int, auditTaskId: Int, gsvPanoramaId: String, labelType: String,
                          lat: Float, lng: Float)
/**
 *
 */
class LabelTable(tag: Tag) extends Table[Label](tag, Some("sidewalk"), "label") {
  def labelId = column[Int]("label_id", O.PrimaryKey, O.AutoInc)
  def auditTaskId = column[Int]("audit_task_id", O.NotNull)
  def gsvPanoramaId = column[String]("gsv_panorama_id", O.NotNull)
  def labelTypeId = column[Int]("label_type_id", O.NotNull)
  def photographerHeading = column[Float]("photographer_heading", O.NotNull)
  def photographerPitch = column[Float]("photographer_pitch", O.NotNull)
  def panoramaLat = column[Float]("panorama_lat", O.NotNull)
  def panoramaLng = column[Float]("panorama_lng", O.NotNull)
  def deleted = column[Boolean]("deleted", O.NotNull)

  def * = (labelId, auditTaskId, gsvPanoramaId, labelTypeId, photographerHeading, photographerPitch, panoramaLat, panoramaLng, deleted) <> ((Label.apply _).tupled, Label.unapply)

  def auditTask: ForeignKeyQuery[AuditTaskTable, AuditTask] =
    foreignKey("label_audit_task_id_fkey", auditTaskId, TableQuery[AuditTaskTable])(_.auditTaskId)

  def labelType: ForeignKeyQuery[LabelTypeTable, LabelType] =
    foreignKey("label_label_type_id_fkey", labelTypeId, TableQuery[LabelTypeTable])(_.labelTypeId)

}

/**
 * Data access object for the label table
 */
object LabelTable {
  val db = play.api.db.slick.DB
  val labels = TableQuery[LabelTable]

  /**
   * Saves a new labe in the table
   * @param label
   * @return
   */
  def save(label: Label): Int = db.withTransaction { implicit session =>
    val labelId: Int =
      (labels returning labels.map(_.labelId)) += label
    labelId
  }

  /**
   * This method retunrs a list of labels submitted by the given user.
   * @param userId
   * @return
   */
  def submittedLabels(userId: UUID): List[LabelLocation] = db.withSession { implicit session =>
    val auditTasks = TableQuery[AuditTaskTable]
    val labelTypes = TableQuery[LabelTypeTable]
    val _labels = for {
      ((_auditTasks, _labels), _labelTypes) <- auditTasks leftJoin labels on(_.auditTaskId === _.auditTaskId) leftJoin labelTypes on (_._2.labelTypeId === _.labelTypeId)
      if _auditTasks.userId === userId.toString && _labels.deleted === false
    } yield (_labels.labelId, _labels.auditTaskId, _labels.gsvPanoramaId, _labelTypes.labelType, _labels.panoramaLat, _labels.panoramaLng)

//    val _labelsWithLatLng = for {
//      (_labels, _labelTypes) <- labels innerJoin labelTypes on (_.labelTypeId === _.labelTypeId)
//    } yield (_labels.labelId, _labels.auditTaskId, _labels.gsvPanoramaId, _labelTypes.labelType, _labels.panoramaLat, _labels.panoramaLng)

    val labelLocationList: List[LabelLocation] = _labels.list.map(label => LabelLocation(label._1, label._2, label._3, label._4, label._5, label._6))
    labelLocationList
  }
}

