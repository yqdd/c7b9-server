package com.ow0b.midi;

import java.util.Random;
import java.util.Set;

//这里要想相似度不是0需要是同一个minHash对象
public class MinHash
{
    private final int numHashFunctions;
    private final int[] hashA;
    private final int[] hashB;
    private final int prime = 2147483647; //大素数

    public MinHash(int numHashFunctions)
    {
        this.numHashFunctions = numHashFunctions;
        this.hashA = new int[numHashFunctions];
        this.hashB = new int[numHashFunctions];
        Random random = new Random();
        //生成随机系数
        for (int i = 0; i < numHashFunctions; i++)
        {
            hashA[i] = random.nextInt(prime - 1) + 1; // [1, prime-1]
            hashB[i] = random.nextInt(prime);         // [0, prime-1]
        }
    }

    public int[] computeMinHash(Set<String> set)
    {
        int[] minHash = new int[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) minHash[i] = Integer.MAX_VALUE; //使用最大整数值初始化

        //遍历集合中的每个元素
        for (String item : set)
        {
            int itemHash = item.hashCode();
            for (int i = 0; i < numHashFunctions; i++)
            {
                int hashValue = (hashA[i] * itemHash + hashB[i]) % prime;
                minHash[i] = Math.min(minHash[i], hashValue);
            }
        }
        return minHash;
    }

    public double computeSimilarity(int[] minHash1, int[] minHash2)
    {
        if (minHash1.length != minHash2.length)
        {
            throw new IllegalArgumentException("MinHash签名必须具有相同的长度");
        }

        int identicalCount = 0;
        for (int i = 0; i < minHash1.length; i++)
        {
            if (minHash1[i] == minHash2[i]) identicalCount++;
        }
        return (double) identicalCount / minHash1.length;
    }
}