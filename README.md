# Distributed Minimum Spanning Trees and Shortest Paths for Leader Election

This is a system that explores the use of minimum spanning trees and shortest
paths for basic operations in distributed systems, including leader election,
broadcasting, and querying.

## Honor Code
We affirm our awareness of the standards of the Harvard College Honor Code.

## Obtaining the Code

Clone the [Github repo](https://github.com/KevinWang94/262FinalProject).
All paths are now relative to the top-level directory.
``` 
git clone https://github.com/KevinWang94/262FinalProject
```


## Running

### Creating a Runnable JAR

LUCY THIS IS FOR YOU

### Running

Run the created JAR at the command line with the following arguments:

XX 
## Overview

This is a simulation system meant to capture basic operations in a distributed
system: leader election, broadcast from the leader, and a query from machines
to the leader. It includes implementations of these features through three methods:
a random choice of leader and direct broadcasts / queries, a choice of leader on
an MST and broadcasts / queries via the tree structure of the MST, and a choice
of leader made to minimize the cost of querying via the computation of shortest paths.

The system proceeds as follows. In the first stage, our base system first
chooses a leader on startup. Once all processes acknowledge to the leader that
they know which machine is the leader, the leader then moves to the second
stage, sending a broadcast message to all processes. Upon receipt of this
broadcast, all processes then move to the third stage, sending a query message 
to the leader. After receiving a query from all machines, the leader sends a 
signal for all machines to shut down and shuts down itself. While this sequence 
of three stages is simple, it allows us to investigate the costs associated with 
the core building block operations of distributed systems in a simulation setting.

## Architecture

We simulated the parallel launch of various machines, each of which has a communication 
channel to any other node in the network. Furthermore, all these communication channels 
(edges of the network) were associated with costs. 

To evaluate the performance of leader election via the three methods above, we created a 
simple distributed system as a base abstract class (in common.Process). This simple 
system was then implemented via a baseline, MST, and shortest path implementation 
(in baseline.BaselineProcess, mst.MSTProcess, and shortestpath.ShortestPathProcess 
respectively). 

The leader election, broadcast, and query operations have complete implementations 
in the packages baseline, mst, and shortestpath, according to the descriptions in 
three papers: Schneider (“What Good are Models”), Gallager et al (1983) and Kanchi (2006).
The progression between the sequence of three stages described in the overview is 
handled in the common.Process, as this workload is shared between all implementations.
However, various operations specific to each basic operation implementation are 
handled in the three implementation packages. All basic operations for baseline 
are implemented in baseline.BaselineProcess, and all basic operations for shortest 
path leader election are implemented in shortestpath.ShortestPathProcess. 
The package util includes a couple data structures used by shortest path leader 
election for storing edges and path information. We split the MST leader election 
out into mst.MSTBase (since this was also useful as a subroutine for shortest path
leader election); however, the other basic operations for the MST approach are 
implemented in mst.MSTProcess.

At the core, these algorithms all involve the exchange of messages. common.Message 
serves as the base class for defining messages exchanged between machines. 
common.MessageContent represents the content of these messages, which is overridden 
in the baseline, mst, and shortestpath packages to reflect particular message 
types needed by these algorithms. 

Machines progress at each step by checking their message queues for a new message, 
and then processing this message. Sometimes, this message is one that reflects a 
change between the three stages handled in common; this message is then processed 
entirely in the base package. However, other additional message types are used to 
signal between machines during the MST leader election or shortest path leader 
election; the processing of these messages is handled in the implementation 
packages. Often, messages are handled differently depending on whether the machine 
is the current leader or not.

To collect the costs associated with these operations in our experiments, we 
implemented a cost tracker in common.CostTracker, which registers the cost of 
messages as they are sent between machines, and then adds and reports these costs 
upon the completion of each operation. 

Finally, common.ElectionRunner is our entry point into the simulation. This class 
launches various machines depending on commandline arguments and performs the three
stages of leader election, broadcast, and querying across our three implementations.
Costs are accumulated and reported at the end of the simulation.

## Files

### Common files
* `CostTracker.java` - tracks costs of communication for basic operations during 
experiments
* `ElectionRunner.java` - main method for launching machines for simulations
* `Message.java` - class for representing messages passed between machines
* `MessageContent.java` - class for representing the content of messages
* `Process.java` - abstract base class capturing the general stages of communication 
between the leader and other machines in the simulations

### baseline
* `BaselineMessageContent.java` - messages specific to our baseline leader election 
* `BaselineProcess.java` - implementation of leader election, broadcast, and query 
via random UUID generation

### mst
* `MSTBase.java` - implementation of leader election via distributed MST generation
* `MSTMessageContent.java` - messages specific to MST leader election
* `MSTProcess.java` - implementation of broadcast and query via connections of the MST

### shortestpath
* `ShortestPathMessageContent.java` - messages specific to shortest path leader election
* `ShortestPathProcess.java` - implementation of leader election, broadcast, and query 
to use shortest paths for communicating queries

### util
* `Pair.java` - used to represent edges in a network of nodes
* `PathInfo.java` - used to represent shortest paths in a node network