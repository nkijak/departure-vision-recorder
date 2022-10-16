# NJ Transit Departure Vision Recorder
[![Build Status](https://travis-ci.org/nkijak/departure-vision-recorder.svg?branch=master)](https://travis-ci.org/nkijak/departure-vision-recorder)

Developed under New York Penn but theoretically should be able to track any with minor up grades.  Crawl Departure Vision mobile page and record departures to couchdb.

## Stations
```
<option value="AB">Absecon</option>
<option value="AZ">Allendale</option>
<option value="AH">Allenhurst</option>
<option value="AS">Anderson Street</option>
<option value="AN">Annandale</option>
<option value="AM">Aberdeen Matawan</option><option value="AP">Asbury Park</option>
<option value="AO">Atco</option>
<option value="AC">Atlantic City</option>
<option value="AV">Avenel</option>
<option value="BI">Basking Ridge</option>
<option value="BH">Bay Head</option>
<option value="MC">Bay Street</option>
<option value="BS">Belmar</option>
<option value="BY">Berkeley Heights</option>
<option value="BV">Bernardsville</option>
<option value="BM">Bloomfield</option>
<option value="BN">Boonton</option>
<option value="BK">Bound Brook</option>
<option value="BB">Bradley Beach</option>
<option value="BU">Brick Church</option>
<option value="BW">Bridgewater</option>
<option value="BF">Broadway</option>
<option value="CB">Campbell Hall</option>
<option value="CM">Chatham</option>
<option value="CY">Cherry Hill</option>
<option value="IF">Clifton</option>
<option value="CN">Convent</option>
<option value="XC">Cranford</option>
<option value="DL">Delawanna</option>
<option value="DV">Denville</option>
<option value="DO">Dover</option>
<option value="DN">Dunellen</option>
<option value="EO">East Orange</option>
<option value="ED">Edison</option>
<option value="EH">Egg Harbor City</option>
<option value="EL">Elberon</option>
<option value="EZ">Elizabeth</option>
<option value="EN">Emerson</option>
<option value="EX">Essex Street</option>
<option value="FW">Fanwood</option>
<option value="FH">Far Hills</option>
<option value="GD">Garfield</option>
<option value="GW">Garwood</option>
<option value="GI">Gillette</option>
<option value="GL">Gladstone</option>
<option value="GG">Glen Ridge</option>
<option value="GK">Glen Rock Boro Hall</option>
<option value="RS">Glen Rock Main Line</option>
<option value="HQ">Hackettstown</option>
<option value="HL">Hamilton</option>
<option value="HN">Hammonton</option>
<option value="RM">Harriman</option>
<option value="HW">Hawthorne</option>
<option value="HZ">Hazlet</option>
<option value="HG">High Bridge</option>
<option value="HI">Highland Avenue</option>
<option value="HD">Hillsdale</option>
<option value="UF">Ho-Ho-Kus</option>
<option value="HB">Hoboken</option>
<option value="JA">Jersey Avenue</option>
<option value="KG">Kingsland</option>
<option value="HP">Lake Hopatcong</option>
<option value="ON">Lebanon</option>
<option value="LP">Lincoln Park</option>
<option value="LI">Linden</option>
<option value="LW">Lindenwold</option>
<option value="FA">Little Falls</option>
<option value="LS">Little Silver</option>
<option value="LB">Long Branch</option>
<option value="LN">Lyndhurst</option>
<option value="LY">Lyons</option>
<option value="MA">Madison</option>
<option value="MZ">Mahwah</option>
<option value="SQ">Manasquan</option>
<option value="MW">Maplewood</option>
<option value="XU">Meadowlands Sports Complex</option>
<option value="MP">Metropark</option>
<option value="MU">Metuchen</option>
<option value="MI">Middletown New Jersey</option>
<option value="MD">Middletown New York</option>
<option value="MB">Millburn</option>
<option value="GO">Millington</option>
<option value="MK">Monmouth Park</option>
<option value="HS">Montclair Heights</option>
<option value="UV">Montclair State University</option>
<option value="ZM">Montvale</option>
<option value="MX">Morris Plains</option>
<option value="MR">Morristown</option>
<option value="HV">Mount Arlington</option>
<option value="OL">Mount Olive</option>
<option value="TB">Mount Tabor</option>
<option value="MS">Mountain Avenue</option>
<option value="ML">Mountain Lakes</option>
<option value="MT">Mountain</option>
<option value="MV">Mountain View</option>
<option value="MH">Murray Hill</option>
<option value="NN">Nanuet</option>
<option value="NT">Netcong</option>
<option value="NE">Netherwood</option>
<option value="NH">New Bridge Landing</option>
<option value="NB">New Brunswick</option>
<option value="NV">New Providence</option>
<option value="NY">New York Penn</option>
<option value="NA">Newark Airport</option>
<option value="ND">Newark Broad Street</option>
<option value="NP">Newark Penn</option>
<option value="OR">North Branch</option>
<option value="NZ">North Elizabeth</option>
<option value="OD">Oradell</option>
<option value="OG">Orange</option>
<option value="OS">Otisville</option>
<option value="PV">Park Ridge</option>
<option value="PS">Passaic</option>
<option value="RN">Paterson</option>
<option value="PC">Peapack</option>
<option value="PQ">Pearl River</option>
<option value="PN">Pennsauken Transit Center</option>
<option value="PE">Perth Amboy</option>
<option value="PH">Philadelphia 30th Street</option>
<option value="PF">Plainfield</option>
<option value="PL">Plauderville</option>
<option value="PP">Point Pleasant Beach</option>
<option value="PO">Port Jervis</option>
<option value="PR">Princeton</option>
<option value="PJ">Princeton Junction</option>
<option value="FZ">Radburn</option>
<option value="RH">Rahway</option>
<option value="RY">Ramsey</option>
<option value="17">Ramsey Route 17</option>
<option value="RA">Raritan</option>
<option value="RB">Red Bank</option>
<option value="RW">Ridgewood</option>
<option value="RG">River Edge</option>
<option value="RL">Roselle Park</option>
<option value="RF">Rutherford</option>
<option value="CW">Salisbury Mills Cornwall</option>
<option value="TS">Secaucus Lower Level</option>
<option value="SE">Secaucus Upper Level</option>
<option value="RT">Short Hills</option>
<option value="XG">Sloatsburg</option>
<option value="SM">Somerville</option>
<option value="CH">South Amboy</option>
<option value="SO">South Orange</option>
<option value="LA">Spring Lake</option>
<option value="SV">Spring Valley</option>
<option value="SG">Stirling</option>
<option value="SF">Suffern</option>
<option value="ST">Summit</option>
<option value="TE">Teterboro</option>
<option value="TO">Towaco</option>
<option value="TR">Trenton Transit Center</option>
<option value="TC">Tuxedo</option>
<option value="US">Union</option>
<option value="UM">Upper Montclair</option>
<option value="WK">Waldwick</option>
<option value="WA">Walnut Street</option>
<option value="WG">Watchung Avenue</option>
<option value="WT">Watsessing Avenue</option>
<option value="23">Wayne/Route 23 Transit Center</option>
<option value="WM">Wesmont</option>
<option value="WF">Westfield</option>
<option value="WW">Westwood</option>
<option value="WH">White House</option>
<option value="WR">Wood Ridge</option>
<option value="WB">Woodbridge</option>
<option value="WL">Woodcliff Lake</option>
```
# NJTransit redesign
this lists all the functions:
https://traindata.njtransit.com/NJTSignData.asmx?username=DV4&password=ekd0J4MsYgmN56&station=TR&status=n

here's an interesting late train list on all lines
https://traindata.njtransit.com/NJTSignData.asmx/getLateTrainJSON?username=DV4&password=ekd0J4MsYgmN56&station=TR&status=n&NJTOnly=true
