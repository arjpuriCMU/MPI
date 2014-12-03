import sys
import csv
import numpy
import getopt
import math

def usage():
    print '$> python generateDNAdata.py <required args> [optional args]\n' + \
        '\t-c <#>\t\tNumber of clusters to generate\n' + \
        '\t-p <#>\t\tNumber of points per cluster\n' + \
        '\t-o <file>\tFilename for the output of the raw data\n' + \
        '\t-l [#]\t\tLength of DNA strands\n'  

       
       

def similarity(s1, s2):
    '''
    Takes two strands and computes the similarity between them.
    '''
    count = 0
    for c1,c2 in zip(s1,s2):
        if c1 == c2:
            count = count + 1
    return count

def tooClose(strand, centroids, maxSimilarity):
    '''
    Computes the similarity between the strand and all strans
    in the list, and if any strands in the list are closer than minDist,
    this method returns true.
    '''
    for centroid in centroids:
        if similarity(strand, centroid) > maxSimilarity:
                return True

    return False

def handleArgs(args):
    # set up return values
    numClusters = -1
    numPoints = -1
    output = None
    length = 20

    try:
        optlist, args = getopt.getopt(args[1:], 'c:p:v:o:')
    except getopt.GetoptError, err:
        print str(err)
        usage()
        sys.exit(2)

    for key, val in optlist:
        # first, the required arguments
        if   key == '-c':
            numClusters = int(val)
        elif key == '-p':
            numPoints = int(val)
        elif key == '-o':
            output = val
        # now, the optional argument
        elif key == '-l':
            length = int(val)

    # check required arguments were inputted  
    if numClusters < 0 or numPoints < 0 or \
            length < 1 or \
            output is None:
        usage()
        sys.exit()
    return (numClusters, numPoints, output, \
            length)

def drawOrigin(length):
    return numpy.random.choice(['A','G','C','T'],length)

# start by reading the command line
numClusters, \
numPoints, \
output, \
length = handleArgs(sys.argv)

writer = csv.writer(open(output, "w"))

# step 1: generate each DNA centroid
centroids_radii = []
maxSimilarity = length/2   #centroids at most 50% same
for i in range(0, numClusters):
    centroid_radius = drawOrigin(length)
    # is it far enough from the others?
    while (tooClose(centroid_radius, centroids_radii, maxSimilarity)):
        centroid_radius = drawOrigin(length)
    centroids_radii.append(centroid_radius)

# step 2: generate the points for each centroid
points = []
minBasesChanged = 0
maxBasesChanged = length / 4
for i in range(0, numClusters):
    cluster = centroids_radii[i]
    for j in range(0, numPoints):
        # generate random number of bases to be changed
        numChanged = numpy.random.randInt(maxBasesChanged)
        # generate randomly which bases to change
        changedIndexes = numpy.random.choice(length,numChanged)

        # set new strand equal to cluster and change each base chosen
        strand = cluster
        for x in changedIndexes:
            if strand[x] == 'A':
                strand[x] = numpy.random.choice(['G','C','T'])
            elif strand[x] == 'G':
                strand[x] = numpy.random.choice(['A','C','T'])
            elif strand[x] == 'C':
                strand[x] = numpy.random.choice(['A','G','T'])
            else:
                strand[x] = numpy.random.choice(['A','G','C'])
                
        # write the strand out
        writer.writerow("".join(strand))
