package com.ow0b.midi.library;

import com.ow0b.midi.analyzer.Analyzer;
import com.ow0b.midi.MidiImpl;
import com.ow0b.midi.analyzer.group.NoteGroup;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class MidiSWLibrary implements Library
{
    public Map<String, Analyzer> analyzers = new HashMap<>();
    public MidiSWLibrary(File path)
    {
        log.info("加载midi库中...");
        try
        {
            Files.walkFileTree(path.toPath(), new SimpleFileVisitor<>()
            {
                @NotNull
                @Override
                public FileVisitResult visitFile(Path libFile, @NotNull BasicFileAttributes attrs) throws IOException
                {
                    try(InputStream stream = new FileInputStream(libFile.toFile()))
                    {
                        String name = libFile.toFile().getName();
                        analyzers.put(name, new MidiImpl(name, stream.readAllBytes()).analyzer());
                        return FileVisitResult.CONTINUE;
                    }
                    catch (SimilarityTooLowException e)
                    {
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Analyzer findFromName(String name)
    {
        return analyzers.get(name);
    }
    @Override
    public List<FindItem> findAll(Analyzer analyzer, float limit, float deviation, @Nullable Consumer<String> infoConsumer)
    {
        List<FindItem> result = new ArrayList<>();
        int i = 0;
        for(Analyzer a2 : analyzers.values())
        {
            if(infoConsumer != null) infoConsumer.accept("遍历寻找匹配项中：" + i + " （" + a2.getMidi().getName() + "）");
            try
            {
                Range range = rangeOf(analyzer, a2, 0.4f, 0);   //0.01f
                if(range != null)
                    result.add(new FindItem(a2, a2.getMidi().getName(), range));
            }
            catch (SimilarityTooLowException ignore) { }
            i ++;
        }

        result.sort((i1, i2) -> -Float.compare(i1.range.value, i2.range.value));
        return result;
    }
    record MaxIndex(float value, int x, int y) {}
    public @Nullable Range rangeOf(Analyzer self, Analyzer ref, float limit, float deviation) throws SimilarityTooLowException
    {
        ArrayList<List<Float>> indexes = new ArrayList<>();
        TreeSet<MaxIndex> maxs = new TreeSet<>(Comparator.comparingDouble(m -> -m.value));
        log.debug("正在计算矩阵...");
        for(int i = 0; i < self.getGroups().size(); i++)
        {
            NoteGroup refGroup = self.getGroups().get(i);
            List<Float> vector = new ArrayList<>();
            MaxIndex max = null;
            for(int j = 0; j < ref.getGroups().size(); j ++)
            {
                float sim = refGroup.similarity(ref.getGroups().get(j));
                if(max == null || sim > max.value)
                    max = new MaxIndex(sim, i, j);
                vector.add(sim);
            }
            if(max != null && max.value > limit)
                maxs.add(max);

            indexes.add(vector);
        }
        if(maxs.isEmpty()) throw new SimilarityTooLowException("");
        //int[] xArray = maxs.stream().mapToInt(m -> m.x).toArray();
        //int median = median(xArray);     //中位数
        //float var = variance(xArray);    //方差
        //maxs.removeIf(m -> Math.abs(m.x - median) < var);
        log.debug("{}",  maxs);

        log.debug("开始行走...");
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
            float smooth = (float) result.size() / result.stream().mapToInt(i -> i == 0 ? 2 : i).sum()
                    * ((float) Math.min(self.getGroups().size(), result.size()) / Math.max(self.getGroups().size(), result.size()));
            int refValueSizeSum = leftRef.refValue.size() + rightRef.refValue.size();
            float sim = leftRef.value * leftRef.refValue.size() / refValueSizeSum +
                    rightRef.value * rightRef.refValue.size() / refValueSizeSum;
            float value = sim * 0.5f + smooth * 0.5f;

            float start = ref.getGroups().get(Math.min(leftRef.end + result.get(0), ref.getGroups().size() - 1)).start(),
                    end = ref.getGroups().get(Math.max(rightRef.end - result.get(result.size() - 1), 0)).end();
            if(end > start)
            {
                Range range = new Range(leftRef.end, rightRef.end,
                        start, end,
                        itemGroups(ref, result, leftRef.end, self.getGroups().size()),
                        result, value, sim, smooth);
                ranges.add(range);
                if(value > 0.3f)
                    log.debug("最终：{} {}  {}  {} {} {}", ref.getMidi().getName(), max.x, max.y, range.startTime, range.endTime, value);
            }
        }
        if(ranges.isEmpty()) throw new SimilarityTooLowException("");
        //System.out.println(ranges.first().sim +  "  " + ranges.first().smooth + "  " + ranges.first().count);
        return ranges.first();
    }
    private List<NoteGroup> itemGroups(Analyzer analyzer, List<Integer> count, int start, int size)
    {
        List<NoteGroup> groups = new ArrayList<>();
        int index = start;
        itemGroupsCount: for (Integer i : count)
        {
            NoteGroup.Builder builder = NoteGroup.builder();
            for (int j = 0; j < i; j++)
            {
                if(index >= analyzer.getGroups().size()) break itemGroupsCount;
                builder.addAll(analyzer.getGroups().get(index).notes);
                index ++;
            }
            groups.add(builder.build());
        }
        while(groups.size() < size) groups.add(new NoteGroup(List.of()));
        return groups;
    }
    record RefData(List<Integer> count, List<Float> refValue, float value, int end) {}
    private RefData refMapData(List<List<Float>> indexes, List<List<int[]>> paths, boolean left)
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
        //按列数获取每一行对应多少个ref中的元素
        int end = best.get(0)[1];
        LinkedList<Integer> refMap = new LinkedList<>();
        LinkedList<Float> refValue = new LinkedList<>();
        refValue.add(0f);
        refMap.add(1);
        for(int i = 1; i < best.size(); i++)
        {
            refValue.set(refValue.size() - 1, Math.max(refValue.getLast(), indexes.get(best.get(i)[0]).get(best.get(i)[1])));
            if(best.get(i)[1] == best.get(i - 1)[1])
            {
                refMap.set(refMap.size() - 1, refMap.getLast() + 1);
            }
            else
            {
                refValue.add(0f);
                refMap.add(best.get(i)[0] == best.get(i - 1)[0] ? 0 : 1);
            }
            if(i == best.size() - 1)
                end = best.get(i)[1];
        }
        //获取该路径的值总和
        //float value = (float) (best.stream().mapToDouble(xy -> indexes.get(xy[0]).get(xy[1])).sum() / best.size());
        float value = (float) (refValue.stream().mapToDouble(f -> f).sum()) / refValue.size();
        if(left)
        {
            Collections.reverse(refMap);
            refMap.removeLast();        //left最后一位和right第一位有重叠，应该加在一起
            //value -= indexes.get(best.get(0)[0]).get(best.get(0)[1]);   //方便后面value合并计算left直接不要了后面right来补
        }
        return new RefData(refMap, refValue, value, end);
    }
    private List<List<int[]>> walk(List<List<Float>> indexes, int startX, int startY, float deviation, boolean left)
    {
        List<List<int[]>> paths = new ArrayList<>();
        log.trace("初始值：{}, {}", startX, startY);
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
    private List<int[]> nextPoint(List<List<Float>> indexes, int x, int y, float deviation, boolean left)
    {
        int n = left ? -1 : 1;
        TreeSet<MaxIndex> maxs = new TreeSet<>((m1, m2) -> -Float.compare(m1.value, m2.value));
        boolean rightExist = x + n < indexes.size() && x + n >= 0,
                bottomExist = y + n < indexes.get(x).size() - 1 && y + n >= 0;
        if(rightExist) maxs.add(new MaxIndex(indexes.get(x + n).get(y), x + n, y));
        if(bottomExist) maxs.add(new MaxIndex(indexes.get(x).get(y + n), x, y + n));
        if(rightExist && bottomExist) maxs.add(new MaxIndex(indexes.get(x + n).get(y + n), x + n, y + n));
        log.trace("{} {}, {}  {}  {}  {}", left ? "left" : "right", x, y, indexes.get(x).get(y), rightExist, bottomExist);

        List<int[]> result = new LinkedList<>();
        MaxIndex first = maxs.pollFirst(), next;
        if(first != null)
        {
            result.add(new int[] {first.x, first.y});
            /*
            next = maxs.pollFirst();
            while(next != null && Math.abs(next.value - first.value) < deviation)
            {
                result.add(new int[] {next.x, next.y});
                next = maxs.pollFirst();
            }
             */
        }
        return result;
    }
    private int median(int[] array)
    {
        return array[array.length / 2];
    }
    private float variance(int[] array)
    {
        double average = Arrays.stream(array).asDoubleStream().average().orElseThrow();
        double var = Arrays.stream(array).asDoubleStream().map(d -> Math.abs(d - average)).sum() / array.length;
        return (float) var;
    }
}
