package com.metamx.collections.bitmap;

import com.metamx.test.annotation.Benchmark;
import it.uniroma3.mat.extendedset.intset.ConciseSet;
import it.uniroma3.mat.extendedset.intset.ImmutableConciseSet;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.util.BitSet;

@Category({Benchmark.class})
public class RangeBitmapBenchmarkTest extends BitmapBenchmark
{
  @BeforeClass
  public static void prepareRandomRanges() throws Exception
  {
    final BitSet expectedUnion = new BitSet();
    for(int i = 0; i < SIZE; ++i) {
      ConciseSet c = new ConciseSet();
      MutableRoaringBitmap r = new MutableRoaringBitmap();
      {
        int k = 0;
        boolean fill = true;
        while (k < LENGTH) {
          int runLength =  LENGTH / 100 + rand.nextInt(LENGTH / 100);
          for (int j = k; fill && j < LENGTH && j < k + runLength; ++j) {
            c.add(j);
            r.add(j);
            expectedUnion.set(j);
          }
          k += runLength;
          fill = !fill;
        }
      }
      minIntersection = LENGTH / 10;
      for(int k = LENGTH / 2; k < LENGTH / 2 + minIntersection; ++k) {
        c.add(k);
        r.add(k);
        expectedUnion.set(k);
      }
      concise[i] = ImmutableConciseSet.newImmutableFromMutable(c);
      offheapConcise[i] = makeOffheapConcise(concise[i]);
      roaring[i] = r;
      immutableRoaring[i] = makeImmutable(r);
      offheapRoaring[i] = makeOffheap(r);
      genericConcise[i] = new WrappedImmutableConciseBitmap(offheapConcise[i]);
      genericRoaring[i] = new WrappedImmutableRoaringBitmap(offheapRoaring[i]);
    }
    unionCount = expectedUnion.cardinality();
    printSizeStats();
  }
}