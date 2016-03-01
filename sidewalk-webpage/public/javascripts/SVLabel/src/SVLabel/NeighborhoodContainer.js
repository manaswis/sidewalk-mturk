function NeighborhoodContainer (params) {
    var self = { className: "NeighborhoodContainer"},
        neighborhoods = {};

    function _init (params) {
        var i, len, neighborhoodArray = [{"id":0},{"id":1},{"id":2},{"id":3},{"id":4},{"id":5},{"id":6},{"id":7},{"id":8},{"id":9},{"id":10},{"id":11},{"id":12},{"id":13},{"id":14},{"id":15},{"id":16},{"id":17},{"id":18},{"id":19},{"id":20},{"id":21},{"id":22},{"id":23},{"id":24},{"id":25},{"id":26},{"id":27},{"id":28},{"id":29},{"id":30},{"id":31},{"id":32},{"id":33},{"id":34},{"id":35},{"id":36},{"id":37},{"id":38},{"id":39},{"id":40},{"id":41},{"id":42},{"id":43},{"id":44},{"id":45},{"id":46},{"id":47},{"id":48},{"id":49},{"id":50},{"id":51},{"id":52},{"id":53},{"id":54},{"id":55},{"id":56},{"id":57},{"id":58},{"id":59},{"id":60},{"id":61},{"id":62},{"id":63},{"id":64},{"id":65},{"id":66},{"id":67},{"id":68},{"id":69},{"id":70},{"id":71},{"id":72},{"id":73},{"id":74},{"id":75},{"id":76},{"id":77},{"id":78},{"id":79},{"id":80},{"id":81},{"id":82},{"id":83},{"id":84},{"id":85},{"id":86},{"id":87},{"id":88},{"id":89},{"id":90},{"id":91},{"id":92},{"id":93},{"id":94},{"id":95},{"id":96},{"id":97},{"id":98},{"id":99},{"id":100},{"id":101},{"id":102},{"id":103},{"id":104},{"id":105},{"id":106},{"id":107},{"id":108},{"id":109},{"id":110},{"id":111},{"id":112},{"id":113},{"id":114},{"id":115},{"id":116},{"id":117},{"id":118},{"id":119},{"id":120},{"id":121},{"id":122},{"id":123},{"id":124},{"id":125},{"id":126},{"id":127},{"id":128},{"id":129},{"id":130},{"id":131},{"id":132},{"id":133},{"id":134},{"id":135},{"id":136},{"id":137},{"id":138},{"id":139},{"id":140},{"id":141},{"id":142},{"id":143},{"id":144},{"id":145},{"id":146},{"id":147},{"id":148},{"id":149},{"id":150},{"id":151},{"id":152},{"id":153},{"id":154},{"id":155},{"id":156},{"id":157},{"id":158},{"id":159},{"id":160},{"id":161},{"id":162},{"id":163},{"id":164},{"id":165},{"id":166},{"id":167},{"id":168},{"id":169},{"id":170},{"id":171},{"id":172},{"id":173},{"id":174},{"id":175},{"id":176},{"id":177},{"id":178},{"id":179},{"id":180},{"id":181},{"id":182},{"id":183},{"id":184},{"id":185},{"id":186},{"id":187},{"id":188},{"id":189},{"id":190},{"id":191}];
        for (i = neighborhoodArray.length - 1; i >= 0; i--) {
            neighborhoods[neighborhoodArray[i]] = new Neighborhood({
                id: neighborhoodArray[i].id
            });
        }
    }

    function getNeighborhood (id) {
        return id in neighborhoods ? neighborhoods[id] : null;
    }

    /** Return a list of neighborhood ids */
    function getNeighborhoodIds () {
        return Object.keys(neighborhoods).map(function (x) { return parseInt(x, 10); });
    }

    _init();
    return self;
}