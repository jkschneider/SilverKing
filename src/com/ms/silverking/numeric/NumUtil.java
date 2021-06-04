package com.ms.silverking.numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import com.ms.silverking.collection.Pair;

public class NumUtil {
  public static void add(int[] vals, int constant) {
    for (int i = 0; i < vals.length; i++) {
      vals[i] += constant;
    }
  }

  public static void sub(int[] vals, int constant) {
    add(vals, -constant);
  }

  public static double sum(List<Double> lx) {
    double sum;

    sum = 0.0;
    for (double x : lx) {
      sum += x;
    }
    return sum;
  }

  public static int sum(int[] lx) {
    int sum;

    sum = 0;
    for (int x : lx) {
      sum += x;
    }
    return sum;
  }

  public static BigDecimal sum(List<BigDecimal> lx, MathContext mc) {
    BigDecimal sum;

    sum = BigDecimal.ZERO;
    for (BigDecimal x : lx) {
      sum = sum.add(x, mc);
    }
    return sum;
  }

  public static List<BigDecimal> doubleToBD(List<Double> listDouble, MathContext mc) {
    List<BigDecimal> listBD;

    listBD = new ArrayList<>(listDouble.size());
    for (Double d : listDouble) {
      listBD.add(new BigDecimal(d, mc));
      //          listBD.add(new BigDecimal(Double.toString(d), mc));
    }
    return listBD;
  }

  public static List<BigDecimal> normalizeAsBD(List<Double> lx, MathContext mc) {
    List<BigDecimal> nx;
    BigDecimal sum;
    List<BigDecimal> _lx;

    _lx = doubleToBD(lx, mc);
    nx = new ArrayList<>(lx.size());
    sum = sum(_lx, mc);
    for (BigDecimal x : _lx) {
      //if (x < 0.0 || Double.isInfinite(x) || Double.isNaN(x)) {
      if (x.compareTo(BigDecimal.ZERO) < 0) {
        throw new RuntimeException("bad value: " + x);
      }
      //nx.add(x / sum);
      if (sum.compareTo(BigDecimal.ZERO) == 0) {
        throw new IllegalArgumentException("Sum of weights to normalize cannot be zero");
      }
      nx.add(x.divide(sum, mc));
    }
    return nx;
  }

  public static int log2OfPerfectPower(int v) {
    if (v <= 0) {
      throw new ArithmeticException();
    }
    return Integer.numberOfTrailingZeros(v);
  }

  // FUTURE - use DeBruijn sequence
  public static int log2(int x) {
    if (x <= 0) {
      throw new ArithmeticException();
    }
    return Integer.numberOfTrailingZeros(Integer.highestOneBit(x));
  }

  public static boolean isPowerOf2(int x) {
    return Integer.bitCount(x) == 1;
  }

  public static double log(double base, double x) {
    return Math.log(x) / Math.log(base);
  }

  public static int log(int base, int x) {
    return (int) log((double) base, (double) x);
  }

  public static int pow(int base, int exp) {
    return (int) pow((long) base, (long) exp);
  }

  public static long pow(long base, long exp) {
    long result;

    result = 1;
    while (exp != 0) {
      if ((exp & 1) != 0) {
        result *= base;
      }
      exp >>= 1;
      base *= base;
    }

    return result;
  }
    
    /*
    public static void main(String[] args) {
        for (int i = 0; i < 31; i++) {
            int    v;
            
            v = 1 << i;
            try {
                System.out.println(i +"\t"+ v +"\t"+ log2OfPerfectPower(v));
            } catch (RuntimeException re) {
                System.out.println(i +"\t"+ v +"\t"+ re);
            }
        }
    }
    */

  public static int longHashCode(long l) {
    return (int) (l ^ (l >>> 32));
  }

  public static long addWithClamp(long a, long b) {
    long c;

    c = a + b;
    if (a > 0 && b > 0) {
      if (c < 0) {
        return Long.MAX_VALUE;
      } else {
        return c;
      }
    } else if (a < 0 && b < 0) {
      if (c > 0) {
        return Long.MIN_VALUE;
      } else {
        return c;
      }
    } else {
      return c;
    }
  }

  public static int compare(Pair<Long, Long> a, Pair<Long, Long> b) {
    if (a.getV1() < b.getV1()) {
      return -1;
    } else if (a.getV1() > b.getV1()) {
      return 1;
    } else {
      if (a.getV2() < b.getV2()) {
        return -1;
      } else if (a.getV2() > b.getV2()) {
        return 1;
      } else {
        return 0;
      }
    }
  }

  public static int[] parseIntArray(String def, String delimiter) {
    String[] defs;
    int[] vals;

    defs = def.split(delimiter);
    vals = new int[defs.length];
    for (int i = 0; i < defs.length; i++) {
      vals[i] = Integer.parseInt(defs[i]);
    }
    return vals;
  }

  public static int bound(int v, int min, int max) {
    if (v <= min) {
      return min;
    } else if (v >= max) {
      return max;
    } else {
      return v;
    }
  }

  // rudimentary factorial implementation
  public static BigInteger factorial(int n) {
    BigInteger nf;

    nf = BigInteger.ONE;
    for (int i = 2; i <= n; i++) {
      nf = nf.multiply(BigInteger.valueOf(i));
    }
    return nf;
  }

  // rudimentary combinations
  public static long combinations(int n, int r) {
    return factorial(n).divide(factorial(n - r).multiply(factorial(r))).longValue();
  }

  public static Pair<Long, Long> parseFloatingPoint(String s) {
    if (s == null) {
      return null;
    } else {
      int index;
      long wholePart;
      long decimalPart;

      index = s.indexOf('.');
      if (index < 0) {
        wholePart = Long.valueOf(s);
        decimalPart = 0;
      } else {
        wholePart = Long.valueOf(s.substring(0, index));
        decimalPart = Long.valueOf(s.substring(index + 1));
      }
      return Pair.of(wholePart, decimalPart);
    }
  }

  public static void main(String[] args) {
    for (int i = 0; i <= 16; i++) {
      //int    v;

      //v = i;
      //try {
      // System.out.println(i +"\t"+ v +"\t"+ log2(v));
      //} catch (RuntimeException re) {
      //    System.out.println(i +"\t"+ v +"\t"+ re);
      //}
      System.out.println(pow(2, i));
    }
    System.out.printf("%d\n", factorial(4).longValue());
    System.out.printf("%d\n", combinations(1000, 20));
  }
}

