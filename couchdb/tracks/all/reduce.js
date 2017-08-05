function(key, values, reduce) {
	var latest = values.reduce(function(acc, value) {
		var accDate = new Date(acc.at);
                var valueDate = new Date(value.at);
                return accDate.getTime() > valueDate.getTime()? acc: value;
	});
        var d = new Date(latest.at);
        var depart = new Date(latest.at);
        var parts = latest.departs_at.split(":");
        depart.setHours(parts[0]*1);
        depart.setMinutes(parts[1]*1);
        var delta = depart.getTime() - d.getTime();
        var dS = delta / 1000;
        var dM = dS / 60;
	return [latest.track, latest.departs_at, latest.color, "??:"+dM];
}
