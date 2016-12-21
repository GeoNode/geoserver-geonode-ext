from geoserver.wps import process
from geoscript.geom import Point
from geoscript.feature import Feature
from geoscript.layer import Layer
from java.lang import String
from org.apache.commons.math3.stat.descriptive import DescriptiveStatistics
from org.apache.commons.math3.stat.descriptive import SummaryStatistics
import simplejson as json
import math

@process(
    title = 'Summarize Attribute Values',
    description = 'summarize the values for a particular attribute in the provided features.',
    inputs = {
        'features': (Layer, 'Features to which distance and bearing should be calculated.'),
        'attributeName': (String, 'Attribute to compute stats for')
    },
    outputs = {
        'result': (String, 'json string containing the stats.')
    }
)

def run(features, attributeName):

    #stats = {
    #  type: 'number',
    #  populatedCount: 1462,
    #  totalCount: 0,
    #  uniqueValues: {
    #    '10.5': 4,
    #    '9.7': 1,
    #  },
    #  min: 36.618,
    #  max: 86.2747,
    #  range: 49.6567,
    #  sum: 106165,
    #  mean: 72.6165,
    #  median: 74.2404,
    #  stdDev: 15.324,
    #  variance: 0.0123
    #}
    ds = DescriptiveStatistics()
    ss = SummaryStatistics()

    stats = {
        'type': 'number',
        'populatedCount': 0,
        'totalCount': 0,
        'uniqueValues': {}
    }

    for f in features.features():
        stats['totalCount'] += 1
        val = f.attributes[attributeName]

        if str(val) in stats['uniqueValues']:
            stats['uniqueValues'][str(val)] += 1
        else:
            stats['uniqueValues'][str(val)] = 1

        if val is not None and val != '':
            stats['populatedCount'] += 1
            try:
                val_float = float(val)
                ds.addValue(val_float)
                ss.addValue(val_float)
            except ValueError:
                stats['type'] = 'string'
            except:
                # might be a date object
                stats['type'] = 'string'                    

    if stats['type'] == 'number':
        if ss.getN():
            stats['min'] = ss.getMin()
            stats['max'] = ss.getMax()
            stats['range'] = stats['max'] - stats['min']
            stats['sum'] = ss.getSum()
            stats['mean'] = ss.getMean()
            stats['median'] = ds.getPercentile(50)
            stats['stdDev'] = ds.getStandardDeviation()
            stats['variance'] = ss.getPopulationVariance()
    
    stats['uniqueValueCount'] = len(stats['uniqueValues'])
        
    return json.dumps(stats, allow_nan=False)
