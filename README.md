Greedy-Shrink for Solving FAM

This project is implemented in Java11. This project submission folder has datasets and src folder. Main java file is in src->com->nomade named as application.java. This folder contains 4 datasets (1 real and 3 synthetic) namely ForestCover, US Census, NBA, Yahoo. Code on all datasets were tested in intelliJ IDEA IDE, make sure jdk11 or above is installed.

In execution process, execute the code as datafile k n d N. Where datafile contains all the points in the dataset, k is the size of the solution returned, n is the size of the dataset, d is the dimensionality of the dataset and N is the sample size. The algorithm assumes a uniform distributio of linear utility function and samples N utility functions from that distribution. The values of k are 5, 10, 15, 20, 25, and 30. N is 100000. These are the values where author is used. Output.txt file contains the results of the algorithm. It shows the query time, processing time, average regret ratio, average number of users avoided, memory usage, standard deviation.

For example, to run a forestcover dataset, go to add new configuration, in main class enter com.nomade.Application and make sure jdk is properly installed. In program arguments, write forestCover_dataset.txt 5 1000 11 100000. 
