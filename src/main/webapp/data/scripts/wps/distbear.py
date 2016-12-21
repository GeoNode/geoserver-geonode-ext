import math
from geoserver.wps import process
from geoscript.geom import Point
from geoscript.feature import Feature
from geoscript.layer import Layer

@process(
  title = 'Distance and Bearing',
  description = 'Computes Cartesian distance and bearing from features to an origin.',
  inputs = {
    'origin': (Point, 'Origin from which to calculate distance and bearing.'), 
    'features': (Layer, 'Features to which distance and bearing should be calculated.')
  },
  outputs = {
    'result': (Layer, 'Features with calculated distance and bearing attributes.')
  }
)
def run(origin, features):

  for f in features.features():
    p = f.geom.centroid
    d = p.distance(origin)
    b = 90 - math.degrees(math.atan2(p.y - origin.y, p.x - origin.x))
      
    yield Feature({'point': p, 'distance': d, 'bearing': b})

