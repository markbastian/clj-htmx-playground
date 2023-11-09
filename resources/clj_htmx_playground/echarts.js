function makeChart(targetId, options) {
    var chart = echarts.init(document.getElementById(targetId));
    chart.setOption(options);
}