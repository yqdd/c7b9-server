package com.ow0b.midi;

import com.ow0b.midi.analyzer.group.NoteGroup;
import com.ow0b.midi.analyzer.group.TestNoteGroup;
import org.junit.jupiter.api.Test;

import java.util.*;

class FindAllTest
{
    @Test
    void test()
    {
        List<NoteGroup> ref = List.of(
                new TestNoteGroup(0),
                new TestNoteGroup(1),
                new TestNoteGroup(2),
                new TestNoteGroup(3),
                new TestNoteGroup(4, 1, 2, 5),
                new TestNoteGroup(5, 4, 6, 8),
                new TestNoteGroup(6, 2, 4, 5),
                new TestNoteGroup(7),
                new TestNoteGroup(8),
                new TestNoteGroup(9),
                new TestNoteGroup(10),
                new TestNoteGroup(11),
                new TestNoteGroup(12),
                new TestNoteGroup(13));
        List<NoteGroup> groups = List.of(
                new TestNoteGroup(0, 1, 2, 5),
                new TestNoteGroup(1, 4, 6, 8),
                new TestNoteGroup(2, 2, 4, 5));
        System.out.println(rangeOf(ref, groups, 0.5f, 0.1f));
    }


    record MaxIndex(float value, int x, int y) {}
    public record Range(int startIndex, int endIndex, float startTime, float endTime, List<Integer> count, float value) {}
    public static Range rangeOf(List<NoteGroup> ref, List<NoteGroup> groups, float limit, float deviation)
    {
        ArrayList<List<Float>> indexes = new ArrayList<>();
        TreeSet<MaxIndex> maxs = new TreeSet<>(Comparator.comparingDouble(m -> -m.value));
        System.out.println("正在计算矩阵...");
        for(int i = 0; i < groups.size(); i++)
        {
            NoteGroup refGroup = groups.get(i);
            List<Float> vector = new ArrayList<>();
            MaxIndex max = null;
            for(int j = 0; j < ref.size(); j ++)
            {
                float sim = refGroup.similarity(ref.get(j));
                if(max == null || sim > max.value)
                    max = new MaxIndex(sim, i, j);
                vector.add(sim);
            }
            if(max != null && max.value > limit)
                maxs.add(max);

            indexes.add(vector);
        }
        System.out.println("（下面不是标准表示的矩阵，Xij，i是从上到下数，j是从左到右数）");
        for(List<Float> list : indexes)
            System.out.println(list.stream().map(f -> String.format("%.2f", f)).toList());
        //int[] xArray = maxs.stream().mapToInt(m -> m.x).toArray();
        //int median = median(xArray);     //中位数
        //float var = variance(xArray);    //方差
        //maxs.removeIf(m -> Math.abs(m.x - median) < var);
        System.out.println(maxs);

        System.out.println("开始行走...");
        //开始在最小值点行走遍历，只能往左上或右下行走
        TreeSet<Range> ranges = new TreeSet<>(Comparator.comparingDouble(r -> -r.value));
        for(MaxIndex max : maxs)
        {
            RefData leftRef = refMapData(indexes, walk(indexes, max.x, max.y, deviation, true), true),
                    rightRef = refMapData(indexes, walk(indexes, max.x, max.y, deviation, false), false);
            if(leftRef == null || rightRef == null)
                continue;

            //拼接left和right
            List<Integer> result = leftRef.count;
            result.addAll(rightRef.count);
            float value = leftRef.value / leftRef.count.size() + rightRef.value / rightRef.count.size();
            ranges.add(new Range(leftRef.end, rightRef.end, ref.get(leftRef.end + result.get(0)).start(), ref.get(rightRef.end - result.get(result.size() - 1)).end(), result, value));
            System.out.println("最终：" + max.x + "  " + max.y + "  " + value);
        }
        return ranges.first();
    }
    record RefData(List<Integer> count, float value, int end) {}
    private static RefData refMapData(List<List<Float>> indexes, List<List<int[]>> paths, boolean left)
    {
        //获取相加相似度最大的路径
        List<int[]> best = paths.stream()
                .max(Comparator.comparingDouble(l ->
                        l.stream()
                                .mapToDouble(xy -> indexes.get(xy[0]).get(xy[1]))
                                .sum()))
                .orElse(null);
        if(best == null) return null;

        best.remove(null);
        System.out.println((left ? "left" : "right") + " best： " + best.stream().map(i -> List.of(i[0], i[1])).toList());
        //按列数获取每一行对应多少个ref中的元素
        int end = best.get(0)[1];
        LinkedList<Integer> refMap = new LinkedList<>();
        refMap.add(1);
        for(int i = 1; i < best.size(); i++)
        {
            if(best.get(i)[1] == best.get(i - 1)[1])
                refMap.set(refMap.size() - 1, refMap.getLast() + 1);
            else refMap.add(1);
            if(i == best.size() - 1)
                end = best.get(i)[1];
        }
        //获取该路径的值总和
        double value = best.stream().mapToDouble(xy -> indexes.get(xy[0]).get(xy[1])).sum();

        if(left)
        {
            Collections.reverse(refMap);
            refMap.removeLast();        //left最后一位和right第一位有重叠，应该加在一起
            value -= indexes.get(best.get(0)[0]).get(best.get(0)[1]);   //方便后面value合并计算left直接不要了后面right来补
        }
        return new RefData(refMap, (float) value / best.size(), end);
    }
    private static List<List<int[]>> walk(List<List<Float>> indexes, int startX, int startY, float deviation, boolean left)
    {
        List<List<int[]>> paths = new ArrayList<>();
        System.out.println("初始值：" + startX + ", " + startY);
        paths.add(new ArrayList<>() {{  add(new int[] {startX, startY}); }});
        //基于初始值开始行走
        boolean hasWalk = true;
        walk: while(hasWalk)
        {
            hasWalk = false;
            for(int j = 0; j < paths.size(); j ++)
            {
                List<int[]> path = paths.get(j);
                int[] p = path.get(path.size() - 1);
                if(p != null)
                {
                    hasWalk = true;
                    if(left && p[0] <= 0) break walk;
                    if(!left && p[0] >= indexes.size() - 1) break walk;
                    List<int[]> next = nextPoint(indexes, p[0], p[1], deviation, left);
                    if(next.isEmpty()) path.add(null);      //走不了了用null结尾
                    else
                    {
                        path.add(next.get(0));
                        for(int[] i = new int[] {1}; i[0] < next.size(); i[0]++)        //有多的选择则新创建一条路径
                            paths.add(new ArrayList<>(path) {{ add(next.get(i[0])); }});
                    }
                }
            }
        }
        return paths;
    }
    private static List<int[]> nextPoint(List<List<Float>> indexes, int x, int y, float deviation, boolean left)
    {
        int n = left ? -1 : 1;
        TreeSet<MaxIndex> maxs = new TreeSet<>((m1, m2) -> -Float.compare(m1.value, m2.value));
        boolean rightExist = x + n < indexes.size() && x + n >= 0,
                bottomExist = y + n < indexes.get(x).size() - 1 && y + n >= 0;
        if(rightExist) maxs.add(new MaxIndex(indexes.get(x + n).get(y), x + n, y));
        if(bottomExist) maxs.add(new MaxIndex(indexes.get(x).get(y + n), x, y + n));
        if(rightExist && bottomExist) maxs.add(new MaxIndex(indexes.get(x + n).get(y + n), x + n, y + n));
        System.out.println(x + ", " + y + "  " + indexes.get(x).get(y) + "  " + rightExist + "  " + bottomExist);

        List<int[]> result = new LinkedList<>();
        MaxIndex first = maxs.pollFirst(), next;
        if(first != null)
        {
            result.add(new int[] {first.x, first.y});
            next = maxs.pollFirst();
            while(next != null && Math.abs(next.value - first.value) < deviation)
                result.add(new int[] {next.x, next.y});
        }
        return result;
    }
    private static int median(int[] array)
    {
        return array[array.length / 2];
    }
    private static float variance(int[] array)
    {
        double average = Arrays.stream(array).asDoubleStream().average().orElseThrow();
        double var = Arrays.stream(array).asDoubleStream().map(d -> Math.abs(d - average)).sum() / array.length;
        return (float) var;
    }
    private static int linearity(List<Integer> path)
    {
        int linearity = 0;
        for(int i = 1; i < path.size(); i++)
        {
            linearity += Math.abs(path.get(i) - path.get(i - 1) - 1);
        }
        return linearity;
    }
}
