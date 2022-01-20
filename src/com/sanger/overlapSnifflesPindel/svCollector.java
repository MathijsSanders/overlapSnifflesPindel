package com.sanger.overlapSnifflesPindel;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import com.sanger.intervalTree.*;

public class svCollector implements Collector<svInfo, IntervalST<svInfo>, IntervalST<svInfo>> {

	
	
	@Override
    public Supplier<IntervalST<svInfo>> supplier() {
        return IntervalST::new;
    }
	@Override
    public BiConsumer<IntervalST<svInfo>, svInfo> accumulator() {
        return (tree, sv) -> tree.put(new Interval1D(sv.getStart(), sv.getEnd()), sv);
    }
    @Override
    public BinaryOperator<IntervalST<svInfo>> combiner() {
        return (tree1, tree2) -> {
        	tree2.constructIterable().forEach(i -> tree1.put(new Interval1D(i.getStart(), i.getEnd()), i));
        	return tree1;
        };
    }
    @Override
    public Function<IntervalST<svInfo>, IntervalST<svInfo>> finisher() {
        return Function.identity();
    }
    @Override
    public Set<Characteristics> characteristics() {
        return Set.of(Characteristics.UNORDERED);
    }
}
