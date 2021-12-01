package com.nomade;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Application {
    public static int memoryUsage = 0;

    public static int solutionSizeInput;
    public static int datasetSize;
    public static int datasetDimen;
    public static int sampleSizeInput;

    public static List<Float> samplePoints;
    public static List<Float> utilityFuncSamples = new ArrayList<>();

    public static double getRandomSamplingSize(float epsilonValue, float deltaValue) {
        double returnValueFloat = 3 * Math.log(1.0 / deltaValue) / (epsilonValue * epsilonValue);
        return Math.floor(returnValueFloat);
    }

    public static void println(String s) {
        System.out.println(s);
    }

    public static Node allocNode(int index, Node next) {
        Node node = new Node();
        node.index = index;
        node.next = next;
        return node;
    }

    public static Node createNode(int index, Node next) {
        memoryUsage += Node.SIZE;
        return allocNode(index, next);
    }

    public static List<Float> readSampleFile(String filePath) throws FileNotFoundException {
        List<Float> list = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filePath));
        while (scanner.hasNext()) {
            String word = scanner.next();
            list.add(Float.valueOf(word));
        }
        scanner.close();
        memoryUsage += Float.BYTES * list.size();
        return list;
    }

    public static float getUtilFuncSum(int utilIndex, int rawIndex) {
        float sum = 0;
        for (int i = 0; i < datasetDimen; i++) {
            sum += utilityFuncSamples.get(utilIndex * datasetDimen + i) * samplePoints.get(rawIndex * datasetDimen + i);
        }
        return sum;
    }

    public static void main(String[] args) throws IOException {
        float[] satInD;
        float[] satInS;
        int bestPoint = 0;
        println(String.join(" ", args));

        long startTime = System.nanoTime();

        //initializations
        FileWriter fileWriter = new FileWriter("output.txt");
        PrintWriter fp = new PrintWriter(fileWriter);

        String filename = args[0];
        solutionSizeInput = Integer.parseInt(args[1]);
        datasetSize = Integer.parseInt(args[2]);
        datasetDimen = Integer.parseInt(args[3]);
        sampleSizeInput = Integer.parseInt(args[4]);

        samplePoints = readSampleFile(filename);

        List<Boolean> isResult = new ArrayList<>(datasetSize);
        memoryUsage += 4 * datasetSize;
        List<Node> bestPointsUsers = new ArrayList<>(datasetSize);
        memoryUsage += 4 * datasetSize;

        //Reading the points
        for (int i = 0; i < datasetSize; i++) {
            isResult.add(false);
            bestPointsUsers.add(null);
        }
        println("Size of the dataset: " + datasetSize);
        println("dimensionality of dataset: " + datasetDimen);

        if (solutionSizeInput >= datasetSize) {
            println("The value of k is equal or greater than the data size, continue? (y/n)");
            Scanner scanner = new Scanner(System.in);
            String inputChar = scanner.next();
            println("Memory usage:" + memoryUsage);

            if (inputChar.equals("y")) {
                println("Exit!");
                return;
            }
            solutionSizeInput = datasetSize;
        }

        int samplingSize = 0;
        if (sampleSizeInput == 0) {
            samplingSize = (int) getRandomSamplingSize(0.0001f, 0.1f);
        } else {
            samplingSize = sampleSizeInput;
        }

        println("Value of k: " + solutionSizeInput);
        println("Sampling Size: " + samplingSize);
        println("-----------------");
        fp.println(String.format("k = %d, Sampling Size = %d, input = %s", solutionSizeInput, sampleSizeInput, filename));

        memoryUsage += 4 * samplingSize;

        satInD = new float[samplingSize];
        memoryUsage += 4 * samplingSize;
        satInS = new float[samplingSize];
        memoryUsage += 4 * samplingSize;

        Node nodePtr = null;
        int solutionSize = 0;

        bestPoint = -1;
        if (args.length == 6) {
            println("reading sample");
            utilityFuncSamples = readSampleFile(args[5]);
        }

        // Read utility function samples and finding best points
        if (utilityFuncSamples.isEmpty()) {
            for (int i = 0; i < samplingSize * datasetDimen; i++) {
                utilityFuncSamples.add((float)Math.random());
            }
        }

        for (int i = 0; i < samplingSize; i++) {
            float maxUtility = -1;
            for (int j = 0; j < datasetSize; j++) {
                float utility = getUtilFuncSum(i, j);
                if (utility > maxUtility) {
                    maxUtility = utility;
                    bestPoint = j;
                    satInD[i] = utility;
                    satInS[i] = utility;
                }
            }

            if (Boolean.FALSE.equals(isResult.get(bestPoint))) {
                isResult.set(bestPoint, true);
                solutionSize ++;
                nodePtr = createNode(bestPoint, nodePtr);
            }

            Node bestNode = bestPointsUsers.get(bestPoint);
            bestPointsUsers.set(bestPoint, createNode(i, bestNode));
        }

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        fp.println("Preprocessing time :");
        fp.println(String.format("User time : %f seconds", duration / 1000.0f));
        fp.println(String.format("System time : %d milliseconds\n", duration));
        startTime = System.nanoTime();

        if (solutionSize <= solutionSizeInput) {
            println("Fewer than k points for all the uses!");
            println("Average Regret Ratio: " + 0);
            fp.println("Fewer than k points for all the uses!");
            fp.println("memoryUsage (bytes): " + memoryUsage);
            fp.println("Average Regret Ratio: 0");
            fp.println("\n\n");
            fp.close();
            return;
        }

        //Running the main algorithm
        println("Finding the solution");
        float minValue = 2;
        float sumRegretRatio = 0;
        double lowerBound = -1;
        int iterationsAvoided = 0;
        int usersAvoided = 0;
        int usersIterations = 0;
        for (int i = solutionSize; i > solutionSizeInput; i--) {
            minValue = 2;
            int worstPoint = -1;
            int iterationCount = 0;
            Node worstPointPointer = null;
            Node worstPointPrev = null;
            Node prev = null;
            for (Node iter = nodePtr; iter != null; prev = iter, iter = iter.next) {
                iterationCount++;
                if (iter.value < 0) {
                    worstPoint = iter.index;
                    worstPointPointer = iter;
                    worstPointPrev = prev;
                    break;
                }
                if (lowerBound != -1 && iter.value > lowerBound) {
                    break;
                }

                int n = iter.index;
                int usersIterated = 0;
                float tempSumRegretRatio = sumRegretRatio;

                for (Node usersIterator = bestPointsUsers.get(n); usersIterator != null; usersIterator = usersIterator.next) {
                    usersIterated++;
                    int usrIndex = usersIterator.index;
                    tempSumRegretRatio -= (satInD[usrIndex] - satInS[usrIndex]) / satInD[usrIndex];

                    float maxUtility = -1;
                    for (Node iter2 = nodePtr; iter2 != null; iter2 = iter2.next) {
                        if (iter2.index == n)
                            continue;

                        float utility = getUtilFuncSum(usrIndex, iter2.index);
                        if (utility > maxUtility) {
                            maxUtility = utility;
                        }
                    }
                    tempSumRegretRatio += (satInD[usrIndex] - maxUtility) / satInD[usrIndex];
                }
                usersIterations++;
                usersAvoided += samplingSize - usersIterated;
                float arr = tempSumRegretRatio / samplingSize;
                iter.value = arr;

                if (arr < minValue) {
                    minValue = arr;
                    worstPoint = n;
                    worstPointPointer = iter;
                    worstPointPrev = prev;
                }
            }
            iterationsAvoided += i - iterationCount;

            if (worstPointPrev != null) {
                worstPointPrev.next = worstPointPointer.next;
            } else {
                nodePtr = worstPointPointer.next;
            }

            for (Node usersIterator = bestPointsUsers.get(worstPoint); usersIterator != null; usersIterator = usersIterator.next) {
                int usrIndex = usersIterator.index;
                sumRegretRatio -= (satInD[usrIndex] - satInS[usrIndex]) / satInD[usrIndex];
                bestPoint = -1;
                float maxUtility = -1;
                for (Node iter2 = nodePtr; iter2 != null; iter2 = iter2.next) {
                    int j = iter2.index;
                    float utility = getUtilFuncSum(usrIndex, j);
                    if (utility > maxUtility) {
                        maxUtility = utility;
                        bestPoint = j;
                        satInS[usrIndex] = utility;
                    }
                }

                Node bestNode = bestPointsUsers.get(bestPoint);
                bestPointsUsers.set(bestPoint, allocNode(usrIndex, bestNode));
                sumRegretRatio += (satInD[usrIndex] - satInS[usrIndex]) / satInD[usrIndex];
            }

            bestPointsUsers.set(worstPoint, null);
            for (Node iter1 = nodePtr; iter1 != null; iter1 = iter1.next) {
                for (Node iter2 = iter1; iter2 != null; iter2 = iter2.next) {
                    if (iter1.value > iter2.value) {
                        iter1.swapValue(iter2);
                        iter1.swapIndex(iter2);
                    }
                }
            }

            Node iter = nodePtr;
            Node prev1 = null;
            for (int x = 0; x < 2; x++) {
                if (iter == null) {
                    break;
                }
                prev1 = iter;
                iter = iter.next;
            }

            if (iter != null) {
                int startIndex = iter.index;
                float regretRatio = sumRegretRatio;
                for (Node it = bestPointsUsers.get(startIndex); it != null; it = it.next) {
                    regretRatio -= (satInD[iter.index] - satInS[iter.index]) / satInD[iter.index];

                    float maxUtility = -1;
                    for (Node iter2 = nodePtr; iter2 != null; iter2 = iter2.next) {
                        int j = iter2.index;
                        if (j == startIndex)
                            continue;

                        float utility = getUtilFuncSum(iter.index, j);
                        if (utility > maxUtility) {
                            maxUtility = utility;
                        }
                    }
                    regretRatio += (satInD[iter.index] - maxUtility) / satInD[iter.index];
                }

                float arrval = regretRatio / samplingSize;
                lowerBound = arrval;
                iter.value = arrval;
                prev1.next = iter.next;

                iter.next = nodePtr;
                nodePtr = iter;
                for (; iter.next != null && iter.next.value < iter.value; iter = iter.next) {
                    iter.swapValue();
                    iter.swapIndex();
                }
            }
        }

        duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        fp.println("Query time : ");
        fp.println(String.format("User time : %f seconds", duration / 1000.0f));
        fp.println(String.format("System time : %d milliseconds", duration));
        fp.println("\n");

        println("memoryUsage (bytes):" + memoryUsage);
        println("Average Regret Ratio: " + minValue);

        fp.println(String.format("memoryUsage (bytes): %d", memoryUsage));
        fp.println("\n");

        //calculating more statistics
        if (utilityFuncSamples.size() <= 0) {
            for (int i = 0; i < samplingSize * datasetDimen; i++) {
                utilityFuncSamples.add((float)Math.random());
            }
        }
        for (int i = 0; i < samplingSize; i++) {
            float maxUtility = 0;
            for (int j = 0; j < datasetSize; j++) {
                float utility = getUtilFuncSum(i, j);
                if (utility > maxUtility) {
                    maxUtility = utility;
                    satInD[i] = utility;
                }
            }
        }

        float sumVariance = 0;
        float sampleMRR = 0;
        List<Float> userRrs = new ArrayList<>();
        for (int user = 0; user < samplingSize; user++) {
            float maxUtil = 0;
            for (Node iter = nodePtr; iter != null; iter = iter.next) {
                int i = iter.index;
                float utility = getUtilFuncSum(user, i);
                if (utility > maxUtil) {
                    maxUtil = utility;
                }
            }
            satInS[user] = maxUtil;
            float rr = (satInD[user] - maxUtil) / satInD[user];
            userRrs.add(rr);
            if (rr > sampleMRR) {
                sampleMRR = rr;
            }
            float diff = (rr - minValue);
            sumVariance += diff * diff;
        }
        float variance = sumVariance / samplingSize;
        double sd = Math.sqrt(variance);
        int notWithinSD = 0;

        for (int user = 0; user < samplingSize; user++) {
            float rr = (satInD[user] - satInS[user]) / satInD[user];
            if (rr > minValue + sd || rr < minValue - sd) {
                notWithinSD++;
            }
        }

        List<Float> sortedUsers = userRrs.stream().sorted().collect(Collectors.toList());

        double avgIterAvoided = iterationsAvoided * 1.0 / (solutionSize - solutionSizeInput);
        double avgUsersAvoided = usersAvoided * 1.0 / (usersIterations);
        fp.println(String.format("Average Regret Ratio: %f", minValue));
        fp.println(String.format("Sample sd: %f", sd));
        fp.println(String.format("No within SD: %d", notWithinSD));
        fp.println(String.format("70%%: %f", sortedUsers.get((int) (samplingSize * 0.7))));
        fp.println(String.format("80%%: %f", sortedUsers.get((int) (samplingSize * 0.8))));
        fp.println(String.format("90%%: %f", sortedUsers.get((int) (samplingSize * 0.9))));
        fp.println(String.format("95%%: %f", sortedUsers.get((int) (samplingSize * 0.95))));
        fp.println(String.format("99%%: %f", sortedUsers.get((int) (samplingSize * 0.99))));
        fp.println(String.format("Sample mrr: %f", sampleMRR));
        fp.println(String.format("Average number of points avoided: %f", avgIterAvoided));
        fp.println(String.format("Average number of users avoided: %f", avgUsersAvoided));
        fp.println("\n");
        fp.close();
    }
}
