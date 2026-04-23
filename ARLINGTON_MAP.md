# Arlington Road Network Map Reference

## Map Overview

The system uses a simplified model of the real road network of **Arlington County, Virginia, USA**.

Arlington County is located to the west of Washington D.C., connected to the capital by multiple bridges, and is an important part of the Washington metropolitan area.

---

## Key Areas Covered

### 1. Clarendon
- **Location**: Arlington's commercial center district
- **Node**: `1` - Clarendon Blvd & Wilson Blvd
- **Features**: High-density commercial zone, Orange Line Metro station, dining and shopping
- **Coordinates**: (4.0, 6.0)

### 2. Courthouse
- **Location**: Government office district
- **Node**: `2` - Courthouse Rd & Wilson Blvd
- **Features**: County government buildings, courthouse, Orange Line Metro station
- **Coordinates**: (5.5, 6.5)

### 3. Ballston
- **Location**: Northwest Arlington commercial district
- **Node**: `3` - Fairfax Dr & Wilson Blvd
- **Features**: Shopping mall, Orange and Silver Line Metro stations
- **Coordinates**: (2.5, 6.5)

### 4. Rosslyn
- **Location**: Adjacent to Washington D.C., east side of Key Bridge
- **Node**: `4` - Fort Myer Dr & Wilson Blvd
- **Features**: High-rise cluster, Blue / Orange / Silver Line Metro stations, main entry point to D.C.
- **Coordinates**: (6.5, 6.0)

### 5. Pentagon City
- **Location**: Commercial district near the Pentagon
- **Node**: `5` - Army Navy Dr & S Hayes St
- **Features**: Fashion Centre mall, Blue / Yellow Line Metro stations, near Reagan National Airport
- **Coordinates**: (6.0, 2.5)

### 6. Crystal City
- **Location**: Between the Pentagon and Reagan Airport
- **Node**: `6` - Crystal Dr & 15th St
- **Features**: Amazon HQ2 location, Blue / Yellow Line Metro stations, high-rise apartments and offices
- **Coordinates**: (7.0, 3.0)

### 7. Columbia Pike
- **Location**: Major arterial in south Arlington
- **Nodes**: `7`, `8` - Intersections along Columbia Pike
- **Features**: Multicultural community, bus rapid transit corridor
- **Coordinates**: (3.0, 2.0) - (4.5, 2.5)

---

## Major Roads

### East-West Arterials

#### Wilson Boulevard
- Connects: Ballston -> Clarendon -> Courthouse -> Rosslyn
- Distance: approximately 4.5 km
- Features: Arlington's main commercial corridor, parallel to the Orange / Silver Metro lines

#### Lee Highway
- Connects: Western boundary -> Ballston -> Courthouse
- Distance: approximately 3 km
- Features: Historic major road, Route 29

#### Arlington Boulevard / Route 50
- Connects: Western boundary -> Rosslyn
- Distance: approximately 4 km
- Features: Main east-west highway

#### Columbia Pike
- Connects: Southern neighborhoods
- Distance: approximately 3.3 km
- Features: Multicultural corridor with dedicated bus lanes

### North-South Connectors
- Clarendon <-> Route 50: 1.8 km
- Rosslyn <-> Crystal City: 3.5 km (main north-south axis)
- Crystal City <-> Pentagon City: 0.8 km

---

## Boundary Nodes (Traffic Flow Origins and Destinations)

### Northern Boundary
- **N1**: Lee Highway North Entry (toward Falls Church)
- **N2**: Washington Blvd North Entry (toward McLean)

### Southern Boundary
- **S1**: Columbia Pike South Entry (toward Fairfax County)
- **S2**: Arlington Blvd South Entry (toward Alexandria)

### Eastern Boundary (toward Washington D.C.)
- **E1**: Key Bridge Entry (connecting Georgetown)
- **E2**: Memorial Bridge Entry (connecting Lincoln Memorial)

### Western Boundary
- **W1**: Route 50 West Entry (toward Fairfax)
- **W2**: Lee Hwy West Entry (toward Vienna)

---

## Network Statistics

- **Total nodes**: 20
  - Intersection nodes (with signal lights): 12
  - Boundary nodes (traffic flow origins / destinations): 8

- **Total roads**: 48 directed edges
  - Main arterials: 24
  - Connector roads: 12
  - Boundary connections: 12

- **Coverage area**: approximately 8 km x 8 km

---

## Typical Traffic Flow Scenarios

### Morning Peak (Inbound Commute)
**Main direction**: Western / Southern boundary -> Rosslyn / Crystal City (entering D.C.)

Example flows:
```
W1 (Route 50 West) -> Route 50 -> Rosslyn -> E1 (Key Bridge)
S1 (Columbia Pike South) -> Columbia Pike -> Pentagon City -> Crystal City -> E2 (Memorial Bridge)
```

### Evening Peak (Outbound Commute)
**Main direction**: Rosslyn / Crystal City -> Western / Southern boundary

Example flows:
```
E1 (Key Bridge) -> Rosslyn -> Wilson Blvd -> Ballston -> W2 (Lee Hwy West)
E2 (Memorial Bridge) -> Crystal City -> Pentagon City -> S2 (Arlington Blvd South)
```

### Local Area Traffic
**Main direction**: Between commercial districts

Example flows:
```
Ballston -> Wilson Blvd -> Clarendon -> Courthouse
Pentagon City -> Crystal City -> Rosslyn
```

---

## Practical Usage Recommendations

### Creating Test Flows

Use the API to create traffic flows that simulate real scenarios:

```bash
# Morning peak: inbound from the west
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "W1",
    "destination": "E1",
    "numberOfCars": 150
  }'

# Evening peak: outbound from D.C.
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "E1",
    "destination": "W2",
    "numberOfCars": 180
  }'

# Local area traffic
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "3",
    "destination": "5",
    "numberOfCars": 80
  }'
```

### Key Intersections to Watch

The following intersections are typically traffic bottlenecks:

1. **Rosslyn** (`4`) - Main gateway to and from D.C.
2. **Clarendon** (`1`) - Commercial center where multiple roads converge
3. **Pentagon City** (`5`) - Connects the airport and D.C.
4. **Route 50 & Courthouse Rd** (`9`) - Major arterial intersection

---

## Map References

- **Real map**: [Google Maps - Arlington, VA](https://goo.gl/maps/arlington-va)
- **Metro map**: [WMATA Metro Map](https://www.wmata.com/rider-guide/stations/)
- **Live traffic**: [Arlington County Traffic](https://traffic.arlingtonva.us/)

---

## Notes

1. **Simplified model**: This model is a simplified version of the actual road network and includes only the major roads and intersections.
2. **Approximate distances**: Road distances are approximate values based on real-world measurements.
3. **Signal lights**: All intersection nodes are configured with basic traffic light control.
4. **Capacity assumptions**: All roads use the same capacity parameters (50 vehicles/km) and speed limit (60 km/h).

---

## Future Extensions

Consider adding the following real-world features:

1. **Metro stations**: Add transit nodes at Ballston, Clarendon, Courthouse, Rosslyn, etc.
2. **Reagan Airport**: Add airport traffic flows
3. **The Pentagon**: Add Pentagon entrance roads
4. **GW Parkway**: Add the George Washington Memorial Parkway along the Potomac River
5. **I-395**: Add Interstate 395

---

**Last Updated**: 2026-03-12
**Authors**: Chengkun Liao, Mingjie Shen
