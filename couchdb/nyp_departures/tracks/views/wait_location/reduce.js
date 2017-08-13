function(key, values, rereduce) {
  if (!rereduce) {
    var sum = values.reduce(function(a,b) { return a + b });
    var count = values.length;
    var average = sum * 1.0 / count;
    var tss = values.map(function(a) { return a * a; })
                    .reduce(function(a, b) { return a + b; });
    var sumsqravg =  sum * sum / count;
    return {
      sum: sum,
      count: count,
      avg: average,
      tss: tss,
      sumsqr: sum * sum,
      sumsqravg: sumsqravg,
      stddev: tss - sumsqravg
    };
  } else {
   return values.reduce(function(a,b) {
      var sum = a.sum + b.sum;
      return {
        sum: sum,
        count: a.count + b.count,
        average: (a.sum + b.sum * 1.0) / a.count + b.count,
        tss: a.tss + b.tss,
        sumsqr: sum * sum,
        sumsqravg: sum * sum / (a.count + b.count),
        stddev: (a.tss + b.tss) - (sum * sum / (a.count + b.count))
      };
    });
  }
}
