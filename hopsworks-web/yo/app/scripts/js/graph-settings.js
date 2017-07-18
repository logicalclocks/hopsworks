// Settings file used by vizops, loaded by each graph

var vizopsUpdateInterval = function() { return 2000; }
var vizopsGetUpdateLabel = function() { return '(' + vizopsUpdateInterval()/1000 + ' s)'; };
var _getColor = ['#9a92e8','#d60606' ,'#025ced','#c67a0f', '#2166ac', '#b2182b', '#c60f0f',
                 '#35978f','#01665e','#39d81a','#40004b', '#d6604d',
                 '#9970ab','#c310d3','#5aae61','#1505aa'];

var getBaseChartOptions = function() {
    return {
        chart: {
            "type": "lineWithFocusChart",
            "interpolate": "monotone",
            "height": 330,
            "margin": {
                "top": 50,
                "right": 50,
                "bottom": 50,
                "left": 50
            },
            "x": function(d){ return d.x; },
            "y": function(d){ return d.y; },
            "forceY": [0],
            "duration": 500,
            "showLegend": false,
            "useInteractiveGuideline": true,
            "xAxis": {
              "axisLabel": "Time",
              "rotateLabels": -35,
              "tickFormat": function(d) {
                return d3.time.format("%H:%M:%S")(new Date(d));
              }
            },
            "x2Axis": {
              "tickFormat": function(d) {
                return d3.time.format("%H:%M:%S")(new Date(d));
              }
            },
            "yAxis": {
              "axisLabel": "Default",
              "rotateYLabel": true,
              "tickFormat": function(d) {
                return d3.format("d")(d);
              }
            },
            "yAxis1": {},
            "yAxis2": {},
            "y2Axis": {},

        },
        title: {
            enable: true,
            text: 'Default title'
        },
        subtitle: {
            enable: false,
            text: 'Updates every ' + (vizopsUpdateInterval()/1000) + ' s',
            css: {
                'text-align': 'center',
                'margin': '10px 13px 0px 7px'
            }
        },
        caption: {
            enable: false,
            html: 'Test graph retrieving live updates',
            css: {
                'text-align': 'justify',
                'margin': '10px 13px 0px 7px'
            }
        }
    };
};

// OVERVIEW: Total active tasks running in the application
var vizopsTotalActiveTasksOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Tasks",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
      };
    options.title.text = 'Total Active Tasks';
    options.subtitle.enable = true;
    options.subtitle.text = 'Sum of the tasks that became active during the update interval';

    return options;
};

var vizopsTotalActiveTasksTemplate = function() {
    return [
        {
            values: [],
            key: 'active tasks',
            color: _getColor[1]
        }
    ];
};

// OVERVIEW: Total completed tasks in the application
var vizopsTotalCompletedTasksOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis ={
        "axisLabel": "Completed tasks",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      } ;
    options.title.text = 'Completed Tasks overall';

    return options;
};

var vizopsTotalCompletedTasksTemplate = function() {
    return [
        {
            values: [],
            key: 'completed tasks',
            color: _getColor[2]
        }
    ];
};

// OVERVIEW: Task rate of completion in the application
var vizopsRateOfTaskCompletionOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Task completion rate",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format("d")(d);
        }
    };
    options.title.text = 'Task rate of completion';

    return options;
};

var vizopsRateOfTaskCompletionTemplate = function() {
    return [
        {
            values: [],
            key: 'rate of completion',
            color: _getColor[1]
        }
    ];
};

// OVERVIEW: HDFS read rate and total
var vizopsHDFSReadRateTotalOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'HDFS read bytes overall';

    return options;
};

var vizopsHDFSReadRateTotalTemplate = function() {
    return [
        {
            values: [],
            key: 'rate read bytes',
            color: _getColor[6]
        },
        {
            values: [],
            key: 'total read bytes',
            color: _getColor[10]
        }
    ];
};

// OVERVIEW: HDFS write rate and total
var vizopsHDFSWriteRateTotalOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'HDFS write bytes overall';

    return options;
};

var vizopsHDFSWriteRateTotalTemplate = function() {
    return [
        {
            values: [],
            key: 'rate write bytes',
            color: _getColor[6]
        },
        {
            values: [],
            key: 'total write bytes',
            color: _getColor[10]
        }
    ];
};

// OVERVIEW: CONTAINER MEMORY USED AND TOTAL
var vizopsContainerMemoryUsedTotalOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'Memory across all app containers';

    return options;
};

var vizopsContainerMemoryUsedTotalTemplate = function() {
    return [
        {
            values: [],
            key: 'used',
            color: _getColor[6]
        },
        {
            values: [],
            key: 'max',
            color: _getColor[10]
        }
    ];
};

// DRIVER: Heap used
var vizopsMemorySpaceDriverOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format(".2s")(d);
        }
      };
    options.title.text = 'Heap used';

    return options;
};

var vizopsMemorySpaceDriverTemplate = function() {
    return [
        {
            values: [],
            key: 'avg',
            color: _getColor[2]
        },
        {
            values: [],
            key: 'max',
            color: _getColor[14]
        }
    ];
};

// DRIVER: VCPU used
var vizopsVCPUDriverOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "VCPU usage %",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1%")(d);
        }
      };
    options.title.text = 'VCPU usage';

    return options;
};

var vizopsVCPUDriverTemplate = function() {
    return [
        {
            values: [],
            key: 'vcpu',
            color: _getColor[10]
        }
    ];
};

// DRIVER: RDD CACHE Block Manager
var vizopsRDDCacheDiskSpillOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = "multiChart";
    options.chart.yAxis1 = {
         "axisLabel": "RDD Cache Bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.chart.yAxis2 ={
        "axisLabel": "Disk spill Bytes",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      } ;
    options.title.text = "BlockManager";
    options.subtitle.enable = true;
    options.subtitle.text = 'In case disk spill increases, consider adding executor memory';

    return options;
};

var vizopsRDDCacheDiskSpillTemplate = function() {
    return [
        {
            values: [],
            key: 'rdd cache',
            color: _getColor[3],
            type: "line",
            yAxis: 1
        },
        {
            values: [],
            key: 'disk spill',
            color: _getColor[5],
            type: "line",
            yAxis: 2
        }
    ];
};

// DRIVER: GC TIME
var vizopsGCTimeOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "GC Time(rate in seconds)",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1f")(d);
        }
      };
    options.title.text = 'GC Time';

    return options;
};

var vizopsGCTimeTemplate = function() {
    return [
        {
            values: [],
            key: 'Mark&Sweep',
            color: _getColor[11]
        },
        {
            values: [],
            key: 'Scavenge',
            color: _getColor[8]
        }
    ];
};

// EXECUTOR: HDFS/DISK READ
var vizopsExecutorHDFSDiskReadOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
         "axisLabel": "Read bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.title.text = 'HDFS/Disk read';

    return options;
};

var vizopsExecutorHDFSDiskReadTemplate = function() {
    return [
       {
           values: [],
           key: 'HDFS Read',
           color: _getColor[7]
       },
       {
           values: [],
           key: 'Disk Read',
           color: _getColor[14]
       }
    ];
};

// EXECUTOR: HDFS/DISK WRITE
var vizopsExecutorHDFSDiskWriteOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
         "axisLabel": "Write bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.title.text = 'HDFS/Disk write';

    return options;
};

var vizopsExecutorHDFSDiskWriteTemplate = function() {
    return [
       {
           values: [],
           key: 'HDFS Write',
           color: _getColor[7]
       },
       {
           values: [],
           key: 'Disk Write',
           color: _getColor[14]
       }
    ];
};

// EXECUTOR: GC TIME
var vizopsExecutorGCTimeOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "GC Time(rate in seconds)",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1f")(d);
        }
      };
    options.title.text = 'GC Time';
    options.subtitle.enable = true;
    options.subtitle.text = 'Spending too much time on GC means a lot of RDDs being created, maybe cache? (spark.memory.storageFraction)';

    return options;
};

var vizopsExecutorGCTimeTemplate = function() {
    return [
       {
           values: [],
           key: 'Mark&Sweep',
           color: _getColor[11]
       },
       {
           values: [],
           key: 'Scavenge',
           color: _getColor[8]
       }
    ];
};

// EXECUTOR CPU
var vizopsExecutorCPUOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
        "axisLabel": "VCPU usage %",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1%")(d);
        }
      };
    options.title.text = 'Executor VCPU usage';

    return options;
};

var vizopsExecutorCPUDataTemplate = function() {
    return [
       {
           values: [],
           key: 'vcpu',
           color: _getColor[5]
       }
    ];
};

// EXECUTOR MEMORY
var vizopsExecutorMemoryUsageOptions = function() {
    var options = getBaseChartOptions();
    options.chart.yAxis = {
       "axisLabel": "Average memory",
       "rotateYLabel": true,
       "tickFormat": function(d) {
         return d3.format(".2s")(d);
       }
    };
    options.title.text = 'Executor memory';

    return options;
};

var vizopsExecutorMemoryUsageTemplate = function() {
    return [
       {
           values: [],
           key: 'mean',
           color: _getColor[10]
       }
    ];
};

// EXECUTOR TASK DISTRIBUTION
var vizopsExecutorTaskDistributionOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiBarChart';
    options.chart.showControls = false;
    options.chart.xAxis = {
        "axisLabel": "Executors",
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
    };
    options.chart.yAxis = {
      "axisLabel": "Tasks",
      "rotateYLabel": true,
      "tickFormat": function(d) {
        return d3.format("d")(d);
      }
    };
    options.title.text = 'Task distribution';
    options.subtitle.enable = true;
    options.subtitle.text = 'If task work seems evenly shared, then check task duration per executor';

    return options;
};

var vizopsExecutorTaskDistributionTemplate = function() {
    return [
        {
            key: 'Tasks',
            values: []
        }
    ];
};

// EXECUTOR Peak memory per executor
var vizopsExecutorPeakMemoryOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'multiBarChart';
    options.chart.showControls = false;
    options.chart.xAxis = {
        "axisLabel": "Executors",
        "tickFormat": function(d) {
          return d3.format("d")(d);
        }
    };
    options.chart.yAxis = {
      "axisLabel": "Peak memory",
      "rotateYLabel": true,
      "tickFormat": function(d) {
        return d3.format(".2s")(d);
      }
    };
    options.title.text = 'Peak memory';
    options.subtitle.enable = true;
    options.subtitle.text = 'If max executor memory is not reached by most executors, consider allocating less container memory';

    return options;
};

var vizopsExecutorPeakMemoryTemplate = function() {
    return [
        {
            key: 'Peak memory per executor',
            values: [],
            color: _getColor[5]
        }
    ];
};

// EXECUTOR TOTAL SHUFFLE
var vizopsApplicationShuffleOptions = function() {
    var options = getBaseChartOptions();

    options.chart.type = 'multiBarChart';
    options.chart.showControls = false;
    options.chart.xAxis = {
        "axisLabel": "Executors",
        "tickFormat": function(d) {
          return d;
        }
    };
    options.chart.yAxis = {
      "axisLabel": "Bytes",
      "rotateYLabel": true,
      "tickFormat": function(d) {
        return d3.format(".2s")(d);
      }
    };
    options.chart.showControls = true;
    options.title.text = 'Shuffle Read/Write';

    return options;
};

var vizopsApplicationShuffleTemplate = function() {
    return [
        {
            values: [],
            key: 'read',
            color: _getColor[11]
        },
        {
            values: [],
            key: 'write',
            color: _getColor[16]
        }
    ];
};

// WORKER Physical cpu
var vizopsWorkerPhysicalCpuOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Physical CPU %",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".1f")(d) + '%';
        }
      };
    options.title.text = 'Physical CPU average';

    return options;
};

var vizopsWorkerPhysicalCpuTemplate = function() {
    return [
        {
            values: [],
            key: 'usage',
            color: _getColor[10]
        },
        {
            values: [],
            key: 'iowait',
            color: _getColor[12]
        },
        {
            values: [],
            key: 'idle',
            color: _getColor[13]
        }
    ];
};

// WORKER Memory
var vizopsWorkerMemoryUsageOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
         "axisLabel": "Bytes",
         "rotateYLabel": true,
         "tickFormat": function(d) {
           return d3.format(".2s")(d);
         }
       };
    options.title.text = 'Memory usage average';

    return options;
};

var vizopsWorkerMemoryUsageTemplate = function() {
    return [
        {
            values: [],
            key: 'used',
            color: _getColor[1]
        },
        {
            values: [],
            key: 'available',
            color: _getColor[3]
        }
    ];
};

// WORKER Network
var vizopsWorkerNetworkTrafficOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Bytes rate of change",
        "rotateYLabel": true,
        "tickFormat": function(d) {
          return d3.format(".2s")(d);
        }
      };
    options.title.text = 'Network usage(rate of change)';
    options.subtitle.enable = true;
    options.subtitle.text = 'Average when displaying more than one hosts';

    return options;
};

var vizopsWorkerNetworkTrafficTemplate = function() {
     return [
         {
             values: [],
             key: 'received',
             color: _getColor[3]
         },
         {
             values: [],
             key: 'sent',
             color: _getColor[17]
         }
     ];
 };

// WORKER Disk
var vizopsWorkerDiskUsageOptions = function() {
    var options = getBaseChartOptions();

    options.chart.type = 'multiBarChart';
    options.chart.showControls = false;
    options.chart.xAxis = {
        "axisLabel": "Hosts/Disks",
        "tickFormat": function(d) {
          return d;
        }
    };
    options.chart.yAxis = {
      "axisLabel": "Bytes",
      "rotateYLabel": true,
      "tickFormat": function(d) {
        return d3.format(".2s")(d);
      }
    };
    options.chart.showControls = true;
    options.title.text = 'Disk usage';
    options.subtitle.enable = true;
    options.subtitle.text = 'Per host if global view, per device if the filter is active';

    return options;
};

var vizopsWorkerDiskUsageTemplate = function() {
    return [
         {
             values: [],
             key: 'used',
             color: _getColor[14]
         },
         {
              values: [],
              key: 'free',
              color: _getColor[3]
          }
     ];
 };

// WORKER EXECUTORS PER HOST
var vizopsWorkerExecutorsPerHostOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'pieChart';
    options.chart.labelThreshold = 0.01;
    options.chart.labelSunbeamLayout = true;
    options.chart.donut = true;
    options.chart.donutRatio = 0.3;
    options.chart.labelType = "percent";
    options.chart.color = _getColor;
    options.title.text = 'Executors per host';
    options.chart.margin = { "top": 20, "right": 5, "bottom": 5, "left": 5 };

    return options;
};

var vizopsWorkerExecutorsPerHostTemplate = function() {
    return [];
};

// WORKER Total Tasks PER HOST
var vizopsWorkerCompletedTasksPerHostOptions = function() {
    var options = getBaseChartOptions();
    options.chart.type = 'pieChart';
    options.chart.labelThreshold = 0.01;
    options.chart.labelSunbeamLayout = true;
    options.chart.donut = true;
    options.chart.donutRatio = 0.3;
    options.chart.labelType = "percent";
    options.chart.color = _getColor;
    options.title.text = 'Completed tasks per host';
    options.chart.margin = { "top": 20, "right": 5, "bottom": 5, "left": 5 };

    return options;
};

var vizopsWorkerCompletedTasksPerHostTemplate = function() {
    return [];
};

// STREAMING last received batch records
var vizopsStreamingLastReceivedBatchRecordsOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Records",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format("d")(d);
        }
    };
    options.title.text = 'Last received batch records';
    options.subtitle = {
        enable: true,
        text: 'In case the records drops to 0, then it probably means that the job has finished or failed',
        css: {
            'text-align': 'center',
            'margin': '10px 13px 0px 7px'
        }
    };

    return options;
};

var vizopsStreamingLastReceivedBatchRecordsTemplate = function () {
    return [
        {
            values: [],
            key: 'records',
            color: _getColor[10]
        }
    ];
};

// STREAMING total delay (scheduling and processing) of completed batches
var vizopsStreamingTotalDelayOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "miliseconds",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format(".1f")(d);
        }
    };
    options.title.text = 'Total processing delay(scheduling and processing)';
    options.subtitle = {
        enable: true,
        text: 'Backpressure builds up if the total delay is higher than the batch arrival time',
        css: {
            'text-align': 'center',
            'margin': '10px 13px 0px 7px'
        }
    };

    return options;
};

var vizopsStreamingTotalDelayTemplate = function () {
    return [
        {
            values: [],
            key: 'delay(ms)',
            color: _getColor[17]
        }
    ];
};

// STREAMING total received and total processed records
var vizopsStreamingTotalReceivedProcessedRecordsOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Records",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format("d")(d);
        }
    };
    options.title.text = 'Total records received and processed';
    options.subtitle = {
        enable: true,
        text: 'Big gap between the received and processed records potentially means backpressure',
        css: {
            'text-align': 'center',
            'margin': '10px 13px 0px 7px'
        }
    };

    return options;
};

var vizopsStreamingTotalReceivedProcessedRecordsTemplate = function () {
    return [
        {
            values: [],
            key: 'records received',
            color: _getColor[10]
        },
        {
            values: [],
            key: 'records processed',
            color: _getColor[4]
        }
    ];
};

// STREAMING TOTAL COMPLETED, RUNNING, UNPROCESSED BATCHES
var vizopsStreamingBatchStatisticsOptions = function() {
    var options = getBaseChartOptions();

    options.chart.yAxis = {
        "axisLabel": "Batches",
        "rotateYLabel": true,
        "tickFormat": function(d) {
            return d3.format("d")(d);
        }
    };
    options.title.text = 'Batch statistics';

    return options;
};

var vizopsStreamingBatchStatisticsTemplate = function () {
    return [
        {
            values: [],
            key: 'running',
            color: _getColor[10]
        },
        {
            values: [],
            key: 'completed',
            color: _getColor[4]
        },
        {
            values: [],
            key: 'unprocessed',
            color: _getColor[6]
        }
    ];
};

// STREAMING INPUT VS PROCESSING RATE