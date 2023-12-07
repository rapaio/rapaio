/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
 *    Copyright 2020 Aurelian Tutuianu
 *    Copyright 2021 Aurelian Tutuianu
 *    Copyright 2022 Aurelian Tutuianu
 *    Copyright 2023 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package rapaio.math.tensor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import rapaio.math.tensor.factories.DataFactory;
import rapaio.math.tensor.factories.DoubleDenseCol;
import rapaio.math.tensor.factories.DoubleDenseRow;
import rapaio.math.tensor.factories.DoubleDenseStride;
import rapaio.math.tensor.factories.DoubleDenseStrideView;
import rapaio.math.tensor.factories.FloatDenseCol;
import rapaio.math.tensor.factories.FloatDenseRow;
import rapaio.math.tensor.factories.FloatDenseStride;
import rapaio.math.tensor.factories.FloatDenseStrideView;
import rapaio.math.tensor.factories.IntegerDenseCol;
import rapaio.math.tensor.factories.IntegerDenseStride;
import rapaio.math.tensor.factories.IntegerDenseStrideView;

public class TensorTest {

    @Test
    @Disabled
    void profileTest() {
        var engine = TensorMill.varray();

        int m = 3000;
        int n = 3000;
        double[] array = new double[m * n];
        Random random = new Random(42);
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextGaussian();
        }

        var t1 = engine.ofDouble().stride(Shape.of(m, n), Order.C, array);
        var res1 = t1.copy(Order.C).mm(t1.copy(Order.C).transpose());
        java.lang.System.out.println(res1.shape());
    }

    @Test
    void managerTestRunner() {
        managerTestSuite(TensorMill.barray());
        managerTestSuite(TensorMill.varray());
    }

    private void managerTestSuite(TensorMill eng) {
        new TestSuite<>(this, new DoubleDenseRow(eng)).run();
        new TestSuite<>(this, new DoubleDenseCol(eng)).run();
        new TestSuite<>(this, new DoubleDenseStride(eng)).run();
        new TestSuite<>(this, new DoubleDenseStrideView(eng)).run();

        new TestSuite<>(this, new FloatDenseRow(eng)).run();
        new TestSuite<>(this, new FloatDenseCol(eng)).run();
        new TestSuite<>(this, new FloatDenseStride(eng)).run();
        new TestSuite<>(this, new FloatDenseStrideView(eng)).run();

        new TestSuite<>(this, new IntegerDenseCol(eng)).run();
        new TestSuite<>(this, new IntegerDenseCol(eng)).run();
        new TestSuite<>(this, new IntegerDenseStride(eng)).run();
        new TestSuite<>(this, new IntegerDenseStrideView(eng)).run();
    }

    static class TestSuite<N extends Number, T extends Tensor<N, T>> {

        private final DataFactory<N, T> g;
        private final TensorTest self;

        private TestSuite(TensorTest self, DataFactory<N, T> g) {
            this.g = g;
            this.self = self;
        }

        void run() {
            buildTest();
            testGet();
            testSet();
            testPrinting();
            testReshape();
            testIterators();
            testTranspose();
            testFlatten();
            testSqueezeMoveSwapAxis();
            testCopy();
            testMathUnary();
            testMathBinaryScalar();
            testMathBinaryVector();
            vdotTest();
            mvTest();
            mmTest();
            splitTest();
            chunkTest();
            repeatStackConcatTest();
            aggregateOpsTest();
            statisticsTest();
        }

        void buildTest() {
            var t = g.seq(Shape.of(1));
            N value = g.value(1);
            if (value instanceof Double) {
                assertEquals(t.dtype(), DType.DOUBLE);
            }
            if (value instanceof Float) {
                assertEquals(t.dtype(), DType.FLOAT);
            }
            assertEquals(g.mill(), t.mill());
        }

        void testGet() {
            var t = g.seq(Shape.of(2, 3, 4));
            var val = g.value(0);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 4; k++) {
                        assertEquals(val, t.get(i, j, k), String.format("Error on get[%d,%d,%d] %s", i, j, k, t));
                        val = g.inc(val);
                    }
                }
            }
        }

        void testSet() {
            var t = g.zeros(Shape.of(2, 3, 4));
            N val = g.value(0);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 4; k++) {
                        t.set(val, i, j, k);
                        val = g.inc(val);
                    }
                }
            }
            val = g.value(0);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 4; k++) {
                        assertEquals(val, t.get(i, j, k));
                        val = g.inc(val);
                    }
                }
            }
        }

        void testPrinting() {
            var tensor = g.seq(Shape.of(20, 2, 2, 25));
            assertEquals(tensor.toString(), tensor.toSummary());

            assertEquals("""
                    [[[[   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19 ... ]  \s
                       [  25  26  27  28  29  30  31  32  33  34  35  36  37  38  39  40  41  42  43  44 ... ]] \s
                      [[  50  51  52  53  54  55  56  57  58  59  60  61  62  63  64  65  66  67  68  69 ... ]  \s
                       [  75  76  77  78  79  80  81  82  83  84  85  86  87  88  89  90  91  92  93  94 ... ]]]\s
                     [[[ 100 101 102 103 104 105 106 107 108 109 110 111 112 113 114 115 116 117 118 119 ... ]  \s
                       [ 125 126 127 128 129 130 131 132 133 134 135 136 137 138 139 140 141 142 143 144 ... ]] \s
                      [[ 150 151 152 153 154 155 156 157 158 159 160 161 162 163 164 165 166 167 168 169 ... ]  \s
                       [ 175 176 177 178 179 180 181 182 183 184 185 186 187 188 189 190 191 192 193 194 ... ]]]\s
                     [[[ 200 201 202 203 204 205 206 207 208 209 210 211 212 213 214 215 216 217 218 219 ... ]  \s
                       [ 225 226 227 228 229 230 231 232 233 234 235 236 237 238 239 240 241 242 243 244 ... ]] \s
                      [[ 250 251 252 253 254 255 256 257 258 259 260 261 262 263 264 265 266 267 268 269 ... ]  \s
                       [ 275 276 277 278 279 280 281 282 283 284 285 286 287 288 289 290 291 292 293 294 ... ]]]\s
                     [[[ 300 301 302 303 304 305 306 307 308 309 310 311 312 313 314 315 316 317 318 319 ... ]  \s
                       [ 325 326 327 328 329 330 331 332 333 334 335 336 337 338 339 340 341 342 343 344 ... ]] \s
                      [[ 350 351 352 353 354 355 356 357 358 359 360 361 362 363 364 365 366 367 368 369 ... ]  \s
                       [ 375 376 377 378 379 380 381 382 383 384 385 386 387 388 389 390 391 392 393 394 ... ]]]\s
                     [[[ 400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418 419 ... ]  \s
                       [ 425 426 427 428 429 430 431 432 433 434 435 436 437 438 439 440 441 442 443 444 ... ]] \s
                      [[ 450 451 452 453 454 455 456 457 458 459 460 461 462 463 464 465 466 467 468 469 ... ]  \s
                       [ 475 476 477 478 479 480 481 482 483 484 485 486 487 488 489 490 491 492 493 494 ... ]]]\s
                     [[[ 500 501 502 503 504 505 506 507 508 509 510 511 512 513 514 515 516 517 518 519 ... ]  \s
                       [ 525 526 527 528 529 530 531 532 533 534 535 536 537 538 539 540 541 542 543 544 ... ]] \s
                      [[ 550 551 552 553 554 555 556 557 558 559 560 561 562 563 564 565 566 567 568 569 ... ]  \s
                       [ 575 576 577 578 579 580 581 582 583 584 585 586 587 588 589 590 591 592 593 594 ... ]]]\s
                     [[[ 600 601 602 603 604 605 606 607 608 609 610 611 612 613 614 615 616 617 618 619 ... ]  \s
                       [ 625 626 627 628 629 630 631 632 633 634 635 636 637 638 639 640 641 642 643 644 ... ]] \s
                      [[ 650 651 652 653 654 655 656 657 658 659 660 661 662 663 664 665 666 667 668 669 ... ]  \s
                       [ 675 676 677 678 679 680 681 682 683 684 685 686 687 688 689 690 691 692 693 694 ... ]]]\s
                     [[[ 700 701 702 703 704 705 706 707 708 709 710 711 712 713 714 715 716 717 718 719 ... ]  \s
                       [ 725 726 727 728 729 730 731 732 733 734 735 736 737 738 739 740 741 742 743 744 ... ]] \s
                      [[ 750 751 752 753 754 755 756 757 758 759 760 761 762 763 764 765 766 767 768 769 ... ]  \s
                       [ 775 776 777 778 779 780 781 782 783 784 785 786 787 788 789 790 791 792 793 794 ... ]]]\s
                     [[[ 800 801 802 803 804 805 806 807 808 809 810 811 812 813 814 815 816 817 818 819 ... ]  \s
                       [ 825 826 827 828 829 830 831 832 833 834 835 836 837 838 839 840 841 842 843 844 ... ]] \s
                      [[ 850 851 852 853 854 855 856 857 858 859 860 861 862 863 864 865 866 867 868 869 ... ]  \s
                       [ 875 876 877 878 879 880 881 882 883 884 885 886 887 888 889 890 891 892 893 894 ... ]]]\s
                     [[[ 900 901 902 903 904 905 906 907 908 909 910 911 912 913 914 915 916 917 918 919 ... ]  \s
                       [ 925 926 927 928 929 930 931 932 933 934 935 936 937 938 939 940 941 942 943 944 ... ]] \s
                      [[ 950 951 952 953 954 955 956 957 958 959 960 961 962 963 964 965 966 967 968 969 ... ]  \s
                       [ 975 976 977 978 979 980 981 982 983 984 985 986 987 988 989 990 991 992 993 994 ... ]]]\s
                    ...  ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ... ...\s
                    """, tensor.toContent());
            assertEquals("""
                    [[[[     0     1     2     3     4     5     6     7     8     9    10    11    12    13    14    15    16    17    18    19    20    21    22    23    24 ]   \s
                       [    25    26    27    28    29    30    31    32    33    34    35    36    37    38    39    40    41    42    43    44    45    46    47    48    49 ]]  \s
                      [[    50    51    52    53    54    55    56    57    58    59    60    61    62    63    64    65    66    67    68    69    70    71    72    73    74 ]   \s
                       [    75    76    77    78    79    80    81    82    83    84    85    86    87    88    89    90    91    92    93    94    95    96    97    98    99 ]]] \s
                     [[[   100   101   102   103   104   105   106   107   108   109   110   111   112   113   114   115   116   117   118   119   120   121   122   123   124 ]   \s
                       [   125   126   127   128   129   130   131   132   133   134   135   136   137   138   139   140   141   142   143   144   145   146   147   148   149 ]]  \s
                      [[   150   151   152   153   154   155   156   157   158   159   160   161   162   163   164   165   166   167   168   169   170   171   172   173   174 ]   \s
                       [   175   176   177   178   179   180   181   182   183   184   185   186   187   188   189   190   191   192   193   194   195   196   197   198   199 ]]] \s
                     [[[   200   201   202   203   204   205   206   207   208   209   210   211   212   213   214   215   216   217   218   219   220   221   222   223   224 ]   \s
                       [   225   226   227   228   229   230   231   232   233   234   235   236   237   238   239   240   241   242   243   244   245   246   247   248   249 ]]  \s
                      [[   250   251   252   253   254   255   256   257   258   259   260   261   262   263   264   265   266   267   268   269   270   271   272   273   274 ]   \s
                       [   275   276   277   278   279   280   281   282   283   284   285   286   287   288   289   290   291   292   293   294   295   296   297   298   299 ]]] \s
                     [[[   300   301   302   303   304   305   306   307   308   309   310   311   312   313   314   315   316   317   318   319   320   321   322   323   324 ]   \s
                       [   325   326   327   328   329   330   331   332   333   334   335   336   337   338   339   340   341   342   343   344   345   346   347   348   349 ]]  \s
                      [[   350   351   352   353   354   355   356   357   358   359   360   361   362   363   364   365   366   367   368   369   370   371   372   373   374 ]   \s
                       [   375   376   377   378   379   380   381   382   383   384   385   386   387   388   389   390   391   392   393   394   395   396   397   398   399 ]]] \s
                     [[[   400   401   402   403   404   405   406   407   408   409   410   411   412   413   414   415   416   417   418   419   420   421   422   423   424 ]   \s
                       [   425   426   427   428   429   430   431   432   433   434   435   436   437   438   439   440   441   442   443   444   445   446   447   448   449 ]]  \s
                      [[   450   451   452   453   454   455   456   457   458   459   460   461   462   463   464   465   466   467   468   469   470   471   472   473   474 ]   \s
                       [   475   476   477   478   479   480   481   482   483   484   485   486   487   488   489   490   491   492   493   494   495   496   497   498   499 ]]] \s
                     [[[   500   501   502   503   504   505   506   507   508   509   510   511   512   513   514   515   516   517   518   519   520   521   522   523   524 ]   \s
                       [   525   526   527   528   529   530   531   532   533   534   535   536   537   538   539   540   541   542   543   544   545   546   547   548   549 ]]  \s
                      [[   550   551   552   553   554   555   556   557   558   559   560   561   562   563   564   565   566   567   568   569   570   571   572   573   574 ]   \s
                       [   575   576   577   578   579   580   581   582   583   584   585   586   587   588   589   590   591   592   593   594   595   596   597   598   599 ]]] \s
                     [[[   600   601   602   603   604   605   606   607   608   609   610   611   612   613   614   615   616   617   618   619   620   621   622   623   624 ]   \s
                       [   625   626   627   628   629   630   631   632   633   634   635   636   637   638   639   640   641   642   643   644   645   646   647   648   649 ]]  \s
                      [[   650   651   652   653   654   655   656   657   658   659   660   661   662   663   664   665   666   667   668   669   670   671   672   673   674 ]   \s
                       [   675   676   677   678   679   680   681   682   683   684   685   686   687   688   689   690   691   692   693   694   695   696   697   698   699 ]]] \s
                     [[[   700   701   702   703   704   705   706   707   708   709   710   711   712   713   714   715   716   717   718   719   720   721   722   723   724 ]   \s
                       [   725   726   727   728   729   730   731   732   733   734   735   736   737   738   739   740   741   742   743   744   745   746   747   748   749 ]]  \s
                      [[   750   751   752   753   754   755   756   757   758   759   760   761   762   763   764   765   766   767   768   769   770   771   772   773   774 ]   \s
                       [   775   776   777   778   779   780   781   782   783   784   785   786   787   788   789   790   791   792   793   794   795   796   797   798   799 ]]] \s
                     [[[   800   801   802   803   804   805   806   807   808   809   810   811   812   813   814   815   816   817   818   819   820   821   822   823   824 ]   \s
                       [   825   826   827   828   829   830   831   832   833   834   835   836   837   838   839   840   841   842   843   844   845   846   847   848   849 ]]  \s
                      [[   850   851   852   853   854   855   856   857   858   859   860   861   862   863   864   865   866   867   868   869   870   871   872   873   874 ]   \s
                       [   875   876   877   878   879   880   881   882   883   884   885   886   887   888   889   890   891   892   893   894   895   896   897   898   899 ]]] \s
                     [[[   900   901   902   903   904   905   906   907   908   909   910   911   912   913   914   915   916   917   918   919   920   921   922   923   924 ]   \s
                       [   925   926   927   928   929   930   931   932   933   934   935   936   937   938   939   940   941   942   943   944   945   946   947   948   949 ]]  \s
                      [[   950   951   952   953   954   955   956   957   958   959   960   961   962   963   964   965   966   967   968   969   970   971   972   973   974 ]   \s
                       [   975   976   977   978   979   980   981   982   983   984   985   986   987   988   989   990   991   992   993   994   995   996   997   998   999 ]]] \s
                     [[[ 1,000 1,001 1,002 1,003 1,004 1,005 1,006 1,007 1,008 1,009 1,010 1,011 1,012 1,013 1,014 1,015 1,016 1,017 1,018 1,019 1,020 1,021 1,022 1,023 1,024 ]   \s
                       [ 1,025 1,026 1,027 1,028 1,029 1,030 1,031 1,032 1,033 1,034 1,035 1,036 1,037 1,038 1,039 1,040 1,041 1,042 1,043 1,044 1,045 1,046 1,047 1,048 1,049 ]]  \s
                      [[ 1,050 1,051 1,052 1,053 1,054 1,055 1,056 1,057 1,058 1,059 1,060 1,061 1,062 1,063 1,064 1,065 1,066 1,067 1,068 1,069 1,070 1,071 1,072 1,073 1,074 ]   \s
                       [ 1,075 1,076 1,077 1,078 1,079 1,080 1,081 1,082 1,083 1,084 1,085 1,086 1,087 1,088 1,089 1,090 1,091 1,092 1,093 1,094 1,095 1,096 1,097 1,098 1,099 ]]] \s
                     [[[ 1,100 1,101 1,102 1,103 1,104 1,105 1,106 1,107 1,108 1,109 1,110 1,111 1,112 1,113 1,114 1,115 1,116 1,117 1,118 1,119 1,120 1,121 1,122 1,123 1,124 ]   \s
                       [ 1,125 1,126 1,127 1,128 1,129 1,130 1,131 1,132 1,133 1,134 1,135 1,136 1,137 1,138 1,139 1,140 1,141 1,142 1,143 1,144 1,145 1,146 1,147 1,148 1,149 ]]  \s
                      [[ 1,150 1,151 1,152 1,153 1,154 1,155 1,156 1,157 1,158 1,159 1,160 1,161 1,162 1,163 1,164 1,165 1,166 1,167 1,168 1,169 1,170 1,171 1,172 1,173 1,174 ]   \s
                       [ 1,175 1,176 1,177 1,178 1,179 1,180 1,181 1,182 1,183 1,184 1,185 1,186 1,187 1,188 1,189 1,190 1,191 1,192 1,193 1,194 1,195 1,196 1,197 1,198 1,199 ]]] \s
                     [[[ 1,200 1,201 1,202 1,203 1,204 1,205 1,206 1,207 1,208 1,209 1,210 1,211 1,212 1,213 1,214 1,215 1,216 1,217 1,218 1,219 1,220 1,221 1,222 1,223 1,224 ]   \s
                       [ 1,225 1,226 1,227 1,228 1,229 1,230 1,231 1,232 1,233 1,234 1,235 1,236 1,237 1,238 1,239 1,240 1,241 1,242 1,243 1,244 1,245 1,246 1,247 1,248 1,249 ]]  \s
                      [[ 1,250 1,251 1,252 1,253 1,254 1,255 1,256 1,257 1,258 1,259 1,260 1,261 1,262 1,263 1,264 1,265 1,266 1,267 1,268 1,269 1,270 1,271 1,272 1,273 1,274 ]   \s
                       [ 1,275 1,276 1,277 1,278 1,279 1,280 1,281 1,282 1,283 1,284 1,285 1,286 1,287 1,288 1,289 1,290 1,291 1,292 1,293 1,294 1,295 1,296 1,297 1,298 1,299 ]]] \s
                     [[[ 1,300 1,301 1,302 1,303 1,304 1,305 1,306 1,307 1,308 1,309 1,310 1,311 1,312 1,313 1,314 1,315 1,316 1,317 1,318 1,319 1,320 1,321 1,322 1,323 1,324 ]   \s
                       [ 1,325 1,326 1,327 1,328 1,329 1,330 1,331 1,332 1,333 1,334 1,335 1,336 1,337 1,338 1,339 1,340 1,341 1,342 1,343 1,344 1,345 1,346 1,347 1,348 1,349 ]]  \s
                      [[ 1,350 1,351 1,352 1,353 1,354 1,355 1,356 1,357 1,358 1,359 1,360 1,361 1,362 1,363 1,364 1,365 1,366 1,367 1,368 1,369 1,370 1,371 1,372 1,373 1,374 ]   \s
                       [ 1,375 1,376 1,377 1,378 1,379 1,380 1,381 1,382 1,383 1,384 1,385 1,386 1,387 1,388 1,389 1,390 1,391 1,392 1,393 1,394 1,395 1,396 1,397 1,398 1,399 ]]] \s
                     [[[ 1,400 1,401 1,402 1,403 1,404 1,405 1,406 1,407 1,408 1,409 1,410 1,411 1,412 1,413 1,414 1,415 1,416 1,417 1,418 1,419 1,420 1,421 1,422 1,423 1,424 ]   \s
                       [ 1,425 1,426 1,427 1,428 1,429 1,430 1,431 1,432 1,433 1,434 1,435 1,436 1,437 1,438 1,439 1,440 1,441 1,442 1,443 1,444 1,445 1,446 1,447 1,448 1,449 ]]  \s
                      [[ 1,450 1,451 1,452 1,453 1,454 1,455 1,456 1,457 1,458 1,459 1,460 1,461 1,462 1,463 1,464 1,465 1,466 1,467 1,468 1,469 1,470 1,471 1,472 1,473 1,474 ]   \s
                       [ 1,475 1,476 1,477 1,478 1,479 1,480 1,481 1,482 1,483 1,484 1,485 1,486 1,487 1,488 1,489 1,490 1,491 1,492 1,493 1,494 1,495 1,496 1,497 1,498 1,499 ]]] \s
                     [[[ 1,500 1,501 1,502 1,503 1,504 1,505 1,506 1,507 1,508 1,509 1,510 1,511 1,512 1,513 1,514 1,515 1,516 1,517 1,518 1,519 1,520 1,521 1,522 1,523 1,524 ]   \s
                       [ 1,525 1,526 1,527 1,528 1,529 1,530 1,531 1,532 1,533 1,534 1,535 1,536 1,537 1,538 1,539 1,540 1,541 1,542 1,543 1,544 1,545 1,546 1,547 1,548 1,549 ]]  \s
                      [[ 1,550 1,551 1,552 1,553 1,554 1,555 1,556 1,557 1,558 1,559 1,560 1,561 1,562 1,563 1,564 1,565 1,566 1,567 1,568 1,569 1,570 1,571 1,572 1,573 1,574 ]   \s
                       [ 1,575 1,576 1,577 1,578 1,579 1,580 1,581 1,582 1,583 1,584 1,585 1,586 1,587 1,588 1,589 1,590 1,591 1,592 1,593 1,594 1,595 1,596 1,597 1,598 1,599 ]]] \s
                     [[[ 1,600 1,601 1,602 1,603 1,604 1,605 1,606 1,607 1,608 1,609 1,610 1,611 1,612 1,613 1,614 1,615 1,616 1,617 1,618 1,619 1,620 1,621 1,622 1,623 1,624 ]   \s
                       [ 1,625 1,626 1,627 1,628 1,629 1,630 1,631 1,632 1,633 1,634 1,635 1,636 1,637 1,638 1,639 1,640 1,641 1,642 1,643 1,644 1,645 1,646 1,647 1,648 1,649 ]]  \s
                      [[ 1,650 1,651 1,652 1,653 1,654 1,655 1,656 1,657 1,658 1,659 1,660 1,661 1,662 1,663 1,664 1,665 1,666 1,667 1,668 1,669 1,670 1,671 1,672 1,673 1,674 ]   \s
                       [ 1,675 1,676 1,677 1,678 1,679 1,680 1,681 1,682 1,683 1,684 1,685 1,686 1,687 1,688 1,689 1,690 1,691 1,692 1,693 1,694 1,695 1,696 1,697 1,698 1,699 ]]] \s
                     [[[ 1,700 1,701 1,702 1,703 1,704 1,705 1,706 1,707 1,708 1,709 1,710 1,711 1,712 1,713 1,714 1,715 1,716 1,717 1,718 1,719 1,720 1,721 1,722 1,723 1,724 ]   \s
                       [ 1,725 1,726 1,727 1,728 1,729 1,730 1,731 1,732 1,733 1,734 1,735 1,736 1,737 1,738 1,739 1,740 1,741 1,742 1,743 1,744 1,745 1,746 1,747 1,748 1,749 ]]  \s
                      [[ 1,750 1,751 1,752 1,753 1,754 1,755 1,756 1,757 1,758 1,759 1,760 1,761 1,762 1,763 1,764 1,765 1,766 1,767 1,768 1,769 1,770 1,771 1,772 1,773 1,774 ]   \s
                       [ 1,775 1,776 1,777 1,778 1,779 1,780 1,781 1,782 1,783 1,784 1,785 1,786 1,787 1,788 1,789 1,790 1,791 1,792 1,793 1,794 1,795 1,796 1,797 1,798 1,799 ]]] \s
                     [[[ 1,800 1,801 1,802 1,803 1,804 1,805 1,806 1,807 1,808 1,809 1,810 1,811 1,812 1,813 1,814 1,815 1,816 1,817 1,818 1,819 1,820 1,821 1,822 1,823 1,824 ]   \s
                       [ 1,825 1,826 1,827 1,828 1,829 1,830 1,831 1,832 1,833 1,834 1,835 1,836 1,837 1,838 1,839 1,840 1,841 1,842 1,843 1,844 1,845 1,846 1,847 1,848 1,849 ]]  \s
                      [[ 1,850 1,851 1,852 1,853 1,854 1,855 1,856 1,857 1,858 1,859 1,860 1,861 1,862 1,863 1,864 1,865 1,866 1,867 1,868 1,869 1,870 1,871 1,872 1,873 1,874 ]   \s
                       [ 1,875 1,876 1,877 1,878 1,879 1,880 1,881 1,882 1,883 1,884 1,885 1,886 1,887 1,888 1,889 1,890 1,891 1,892 1,893 1,894 1,895 1,896 1,897 1,898 1,899 ]]] \s
                     [[[ 1,900 1,901 1,902 1,903 1,904 1,905 1,906 1,907 1,908 1,909 1,910 1,911 1,912 1,913 1,914 1,915 1,916 1,917 1,918 1,919 1,920 1,921 1,922 1,923 1,924 ]   \s
                       [ 1,925 1,926 1,927 1,928 1,929 1,930 1,931 1,932 1,933 1,934 1,935 1,936 1,937 1,938 1,939 1,940 1,941 1,942 1,943 1,944 1,945 1,946 1,947 1,948 1,949 ]]  \s
                      [[ 1,950 1,951 1,952 1,953 1,954 1,955 1,956 1,957 1,958 1,959 1,960 1,961 1,962 1,963 1,964 1,965 1,966 1,967 1,968 1,969 1,970 1,971 1,972 1,973 1,974 ]   \s
                       [ 1,975 1,976 1,977 1,978 1,979 1,980 1,981 1,982 1,983 1,984 1,985 1,986 1,987 1,988 1,989 1,990 1,991 1,992 1,993 1,994 1,995 1,996 1,997 1,998 1,999 ]]]]\s
                    """, tensor.toFullContent());

            tensor = g.seq(Shape.of(2, 3));
            assertEquals("""
                    [[ 0 1 2 ] \s
                     [ 3 4 5 ]]\s
                    """, tensor.toContent());
            assertEquals("""
                    [[ 0 1 2 ] \s
                     [ 3 4 5 ]]\s
                    """, tensor.toFullContent());
        }

        void testReshape() {
            var t = g.seq(Shape.of(2, 3, 4));
            assertEquals(Shape.of(6, 4), t.reshape(Shape.of(6, 4)).shape());
            assertEquals(Shape.of(24, 1, 1), t.reshape(Shape.of(24, 1, 1)).shape());
            assertEquals(Shape.of(1, 24, 1, 1), t.reshape(Shape.of(1, 24, 1, 1)).shape());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> t.reshape(Shape.of(3)));
            assertEquals("Incompatible shape size.", ex.getMessage());
        }

        void testIterators() {
            Shape shape = Shape.of(2, 3, 4);
            var t = g.seq(shape);

            var it = t.ptrIterator(Order.C);
            int pos = 0;
            while (it.hasNext()) {
                assertEquals(t.ptrGet(it.nextInt()), t.get(shape.index(Order.C, pos)));
                assertEquals(pos, it.position());
                pos++;
            }

            it = t.ptrIterator(Order.F);
            pos = 0;
            while (it.hasNext()) {
                assertEquals(t.ptrGet(it.nextInt()), t.get(shape.index(Order.F, pos)), t.toString());
                assertEquals(pos, it.position());
                pos++;
            }

            // set values with iterator and check them later
            t = g.seq(shape);
            it = t.ptrIterator(Order.S);
            while (it.hasNext()) {
                t.ptrSet(it.nextInt(), g.value(1));
            }

            it = t.ptrIterator(Order.C);
            while (it.hasNext()) {
                assertEquals(g.value(1), t.ptrGet(it.nextInt()));
            }
            it = t.ptrIterator(Order.F);
            while (it.hasNext()) {
                assertEquals(g.value(1), t.ptrGet(it.nextInt()));
            }
            it = t.ptrIterator(Order.S);
            while (it.hasNext()) {
                assertEquals(g.value(1), t.ptrGet(it.nextInt()));
            }
        }

        void testTranspose() {
            Shape shape = Shape.of(2, 3, 4);
            var t = g.seq(shape);

            var tt = t.transpose().copy();
            assertArrayEquals(new int[] {4, 3, 2}, tt.shape().dims());

            var ttt = tt.transpose();

            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    for (int k = 0; k < 4; k++) {
                        assertEquals(t.get(i, j, k), tt.get(k, j, i));
                        assertEquals(t.get(i, j, k), ttt.get(i, j, k));
                    }
                }
            }
        }

        void testFlatten() {
            Shape shape = Shape.of(2, 3, 4);
            var t = g.seq(shape);

            var f = t.flatten(Order.C);
            assertArrayEquals(new int[] {4 * 3 * 2}, f.shape().dims());

            var itT = t.ptrIterator(Order.C);
            var itF = f.ptrIterator(Order.C);
            while (itF.hasNext()) {
                assertEquals(t.ptrGet(itT.nextInt()), f.ptrGet(itF.nextInt()),
                        "Error at tensor: " + t + ", flatten: " + f);
            }

            var r = t.ravel(Order.C);
            for (int i = 0; i < t.shape().size(); i++) {
                assertEquals(f.get(i), r.get(i));
            }
        }

        void testSqueezeMoveSwapAxis() {
            Shape shape = Shape.of(2, 1, 3, 1, 4, 1);
            var t = g.seq(shape);
            var s = t.squeeze();
            assertArrayEquals(new int[] {2, 3, 4}, s.shape().dims());

            assertTrue(t.squeeze(5).squeeze(3).squeeze(1).deepEquals(s));

            var it1 = t.ptrIterator(Order.C);
            var it2 = s.ptrIterator(Order.C);
            while (it1.hasNext()) {
                int next1 = it1.nextInt();
                int next2 = it2.nextInt();
                assertEquals(t.ptrGet(next1), s.ptrGet(next2), "t: " + t + ", f: " + s);
            }

            assertTrue(t.moveAxis(2, 3).deepEquals(t.swapAxis(2, 3)));
            assertFalse(t.moveAxis(2, 3).deepEquals(t.swapAxis(0, 2)));

            assertTrue(t.swapAxis(0, 2).deepEquals(t.swapAxis(0, 1).swapAxis(1, 2).swapAxis(0, 1)));
            assertTrue(t.moveAxis(0, 2).deepEquals(t.swapAxis(0, 1).swapAxis(1, 2)));
        }

        void testCopy() {
            Shape shape = Shape.of(2, 3, 4);
            var t = g.seq(shape);

            assertTrue(t.deepEquals(t.copy(Order.C)));
            assertTrue(t.deepEquals(t.copy(Order.F)));

            shape = Shape.of(21, 33, 21, 21);
            t = g.random(shape);
            assertTrue(t.deepEquals(t.copy(Order.C)));
            assertTrue(t.deepEquals(t.copy(Order.F)));
        }

        void testMathUnary() {

            var t1 = g.random(Shape.of(41, 31)).sub(g.value(0.5));
            assertTrue(t1.absNew().deepEquals(t1.abs()));
            t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
            assertTrue(t1.absNew(Order.F).deepEquals(t1.copy(Order.C).abs()));
            assertTrue(t1.copy(Order.F).abs().deepEquals(t1.absNew(Order.C)));

            t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
            assertTrue(t1.negateNew().deepEquals(t1.negate()));
            t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
            assertTrue(t1.copy(Order.C).negate().deepEquals(t1.negateNew(Order.F)));
            assertTrue(t1.copy(Order.F).negate().deepEquals(t1.negateNew(Order.C)));

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy().log().deepEquals(t1.logNew()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).log().deepEquals(t1.logNew(Order.F)));
                assertTrue(t1.copy(Order.F).log().deepEquals(t1.logNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).log());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.log1pNew().deepEquals(t1.log1p()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).log1p().deepEquals(t1.log1pNew(Order.F)));
                assertTrue(t1.copy(Order.F).log1p().deepEquals(t1.log1pNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).log1p());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.expNew().deepEquals(t1.exp()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).exp().deepEquals(t1.expNew(Order.F)));
                assertTrue(t1.copy(Order.F).exp().deepEquals(t1.expNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).exp());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.expm1New().deepEquals(t1.copy().expm1()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).expm1().deepEquals(t1.expm1New(Order.F)));
                assertTrue(t1.copy(Order.F).expm1().deepEquals(t1.expm1New(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).expm1());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.sinNew().deepEquals(t1.sin()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).sin().deepEquals(t1.sinNew(Order.F)));
                assertTrue(t1.copy(Order.F).sin().deepEquals(t1.sinNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).sin());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.asinNew().deepEquals(t1.asin()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).asin().deepEquals(t1.asinNew(Order.F)));
                assertTrue(t1.copy(Order.F).asin().deepEquals(t1.asinNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).asin());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.sinhNew().deepEquals(t1.sinh()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).sinh().deepEquals(t1.sinhNew(Order.F)));
                assertTrue(t1.copy(Order.F).sinh().deepEquals(t1.sinhNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).sinh());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.cosNew().deepEquals(t1.cos()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).cos().deepEquals(t1.cosNew(Order.F)));
                assertTrue(t1.copy(Order.F).cos().deepEquals(t1.cosNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).cos());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.acosNew().deepEquals(t1.acos()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).acos().deepEquals(t1.acosNew(Order.F)));
                assertTrue(t1.copy(Order.F).acos().deepEquals(t1.acosNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).acos());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.coshNew().deepEquals(t1.cosh()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).cosh().deepEquals(t1.coshNew(Order.F)));
                assertTrue(t1.copy(Order.F).cosh().deepEquals(t1.coshNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).cosh());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.tanNew().deepEquals(t1.tan()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).tan().deepEquals(t1.tanNew(Order.F)));
                assertTrue(t1.copy(Order.F).tan().deepEquals(t1.tanNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).tan());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.atanNew().deepEquals(t1.atan()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).atan().deepEquals(t1.atanNew(Order.F)));
                assertTrue(t1.copy(Order.F).atan().deepEquals(t1.atanNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).atan());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }

            if (g.dType().isFloat()) {
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.tanhNew().deepEquals(t1.tanh()));
                t1 = g.random(Shape.of(41, 31)).subNew(g.value(0.5));
                assertTrue(t1.copy(Order.C).tanh().deepEquals(t1.tanhNew(Order.F)));
                assertTrue(t1.copy(Order.F).tanh().deepEquals(t1.tanhNew(Order.C)));
            } else {
                var e = assertThrows(IllegalArgumentException.class, () -> g.random(Shape.of(41, 31)).tanh());
                assertEquals("This operation is available only for floating point tensors.", e.getMessage());
            }
        }

        void testMathBinaryScalar() {
            var t = g.seq(Shape.of(2, 3, 2));
            var c = t.addNew(g.value(10));
            assertTrue(t.add(g.value(10)).deepEquals(c));

            t = g.seq(Shape.of(2, 3, 2));
            c = t.subNew(g.value(2));
            assertTrue(t.sub(g.value(2)).deepEquals(c));

            t = g.seq(Shape.of(2, 3, 2));
            c = t.mulNew(g.value(5));
            assertTrue(t.mul(g.value(5)).deepEquals(c));

            t = g.seq(Shape.of(2, 3, 2));
            c = t.divNew(g.value(4));
            assertTrue(t.div(g.value(4)).deepEquals(c));

            t = g.seq(Shape.of(2, 3, 2));
            c = t.copy();
            assertTrue(c.deepEquals(t.add(g.value(10)).sub(g.value(10))));
            assertTrue(c.deepEquals(t.mulNew(g.value(2)).divNew(g.value(2))));
        }

        void testMathBinaryVector() {

            Shape shape = Shape.of(2, 3, 2);

            var t1 = g.seq(shape);
            var c1 = t1.copy();
            var t2 = g.random(shape);
            var c2 = t2.copy();

            assertTrue(c1.addNew(c2).deepEquals(t1.add(t2)));

            t1 = g.seq(shape);
            assertTrue(c1.subNew(c2).deepEquals(t1.sub(t2)));

            t1 = g.seq(shape);
            assertTrue(c1.mulNew(c2).deepEquals(t1.mul(t2)));

            t1 = g.seq(shape);
            assertTrue(c1.divNew(c2).deepEquals(t1.div(t2)));

            t1 = g.seq(shape);
            assertTrue(c1.addNew(c2).subNew(c2).deepEquals(t1.add(t2).sub(t2)));

            t1 = g.seq(shape);
            assertTrue(c1.mulNew(c2).divNew(c2).deepEquals(t1.mul(t2).div(t2)));
        }

        void vdotTest() {

            int vLen = 23;
            Shape shape = Shape.of(50, vLen);
            var t1 = g.seq(shape);

            var vdots = new ArrayList<N>();
            for (var t : t1.chunk(0, true, 1)) {
                t = t.squeeze();
                vdots.add(t.vdot(t));
            }

            for (int i = 0; i < vdots.size(); i++) {
                var sum = g.value(0);
                for (int j = 0; j < vLen; j++) {
                    sum = g.sum(sum, g.value((i * vLen + j) * (i * vLen + j)));
                }
                assertEquals(sum, vdots.get(i), "i: " + i);
            }

            var e = assertThrows(IllegalArgumentException.class, () -> g.seq(Shape.of(10)).vdot(g.seq(Shape.of(20))));
            assertEquals("Operands are not valid for vector dot product (v = Shape: [10], v = Shape: [20]).", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> g.seq(Shape.of(10)).vdot(g.seq(Shape.of(10)), -1, 10));
            assertEquals("Start and end indexes are invalid (start: -1, end: 10).", e.getMessage());

            var t2 = g.seq(Shape.of(50));
            assertEquals(g.value(40425), t2.vdot(t2));
            assertEquals(g.value(4 * 4 + 5 * 5 + 6 * 6), t2.vdot(t2, 4, 7));
        }

        void mvTest() {
            var e = assertThrows(IllegalArgumentException.class, () -> g.seq(Shape.of(10, 1)).mv(g.seq(Shape.of(2))));
            assertEquals("Operands are not valid for matrix-vector multiplication (m = Shape: [10,1], v = Shape: [2]).", e.getMessage());

            var m1 = g.seq(Shape.of(200, 31));
            var v1 = g.seq(Shape.of(31));

            var r1 = m1.mv(v1);
            assertEquals(Shape.of(200), r1.shape());

            for (int i = 0; i < 200; i++) {
                assertEquals(v1.addNew(g.value(i * 31)).vdot(v1), r1.get(i));
            }

            var r2 = v1.unsqueeze(0).mv(v1);
            assertEquals(Shape.of(1), r2.shape());
            assertEquals(v1.vdot(v1), r2.get(0));
        }

        void mmTest() {

            var e = assertThrows(IllegalArgumentException.class, () -> g.seq(Shape.of(2, 3)).mm(g.seq(Shape.of(4, 3))));
            assertEquals("Operands are not valid for matrix-matrix multiplication (m = Shape: [2,3], v = Shape: [4,3]).", e.getMessage());

            e = assertThrows(IllegalArgumentException.class, () -> g.seq(Shape.of(2, 3)).mm(g.seq(Shape.of(3, 2)), Order.S));
            assertEquals("Illegal askOrder value, must be Order.C or Order.F", e.getMessage());

            var t1 = g.seq(Shape.of(31, 42));
            var t2 = g.seq(Shape.of(42, 1));

            var r1 = t1.mm(t2, Order.C);
            var e1 = t1.mv(g.seq(Shape.of(42)));
            assertTrue(e1.deepEquals(r1.squeeze()));

            var r2 = t1.mm(t2, Order.F);
            var e2 = t1.mv(g.seq(Shape.of(42)));
            assertTrue(e2.deepEquals(r2.squeeze()));

            var t3 = g.random(Shape.of(31, 42));
            var t4 = g.random(Shape.of(42, 200));

            var r3 = t3.mm(t4, Order.C);
            var rows = t3.chunk(0, false, 1);
            var cols = t4.chunk(1, false, 1);
            for (int i = 0; i < t3.shape().dim(0); i++) {
                for (int j = 0; j < t4.shape().dim(1); j++) {
                    N expected = rows.get(i).vdot(cols.get(j));
                    N realized = r3.get(i, j);
                    assertEquals(expected, realized, "i: %d, j: %d".formatted(i, j));
                }
            }

            r3 = t3.mm(t4, Order.F);
            rows = t3.chunk(0, false, 1);
            cols = t4.chunk(1, false, 1);
            for (int i = 0; i < t3.shape().dim(0); i++) {
                for (int j = 0; j < t4.shape().dim(1); j++) {
                    assertEquals(r3.get(i, j), rows.get(i).vdot(cols.get(j)));
                }
            }

        }

        void splitTest() {
            var t1 = g.seq(Shape.of(4, 4, 4));

            var split1 = t1.split(0, false, 0, 1, 2);
            assertEquals(3, split1.size());
            assertEquals(Shape.of(4, 4), split1.get(0).shape());
            assertEquals(Shape.of(4, 4), split1.get(1).shape());
            assertEquals(Shape.of(2, 4, 4), split1.get(2).shape());

            split1 = t1.split(0, true, 0, 1, 2);
            assertEquals(3, split1.size());
            assertEquals(Shape.of(1, 4, 4), split1.get(0).shape());
            assertEquals(Shape.of(1, 4, 4), split1.get(1).shape());
            assertEquals(Shape.of(2, 4, 4), split1.get(2).shape());

            var t2 = g.mill().concat(0, split1);
            assertEquals(t1.shape(), t2.shape());
            assertTrue(t1.deepEquals(t2));


            var e = assertThrows(IllegalArgumentException.class, () -> t1.splitAll(true, new int[][] {
                    {0}, {0}, {0}, {0}
            }));
            assertEquals("Indexes length of 4 is not the same as shape rank 3.", e.getMessage());

            var split2 = t1.splitAll(true, new int[][] {{0}, {0}, {0}});
            assertEquals(1, split2.size());
            assertTrue(t1.deepEquals(split2.get(0)));

            var split3 = t1.splitAll(true, new int[][] {{0, 1}, {0, 2}, {0, 3}});
            assertEquals(8, split3.size());
            List<Shape> expectedShapes = List.of(
                    Shape.of(1, 2, 3), Shape.of(1, 2, 1),
                    Shape.of(1, 2, 3), Shape.of(1, 2, 1),
                    Shape.of(3, 2, 3), Shape.of(3, 2, 1),
                    Shape.of(3, 2, 3), Shape.of(3, 2, 1)
            );
            assertEquals(expectedShapes.size(), split3.size());
            for (int i = 0; i < expectedShapes.size(); i++) {
                assertEquals(expectedShapes.get(i), split3.get(i).shape());
            }

            var p1 = g.mill().concat(2, split3.subList(0, 2));
            var p2 = g.mill().concat(2, split3.subList(2, 4));
            var p3 = g.mill().concat(2, split3.subList(4, 6));
            var p4 = g.mill().concat(2, split3.subList(6, 8));

            var p5 = g.mill().concat(1, List.of(p1, p2));
            var p6 = g.mill().concat(1, List.of(p3, p4));

            var p7 = g.mill().concat(0, List.of(p5, p6));

            assertTrue(t1.deepEquals(p7));
        }

        void chunkTest() {
            var t1 = g.seq(Shape.of(13, 13, 19, 17));

            var chunk1 = t1.chunk(0, true, 4);
            assertEquals(4, chunk1.size());
            assertEquals(Shape.of(4, 13, 19, 17), chunk1.get(0).shape());
            assertEquals(Shape.of(4, 13, 19, 17), chunk1.get(1).shape());
            assertEquals(Shape.of(4, 13, 19, 17), chunk1.get(2).shape());
            assertEquals(Shape.of(1, 13, 19, 17), chunk1.get(3).shape());
            assertTrue(t1.deepEquals(g.mill().concat(0, chunk1)));

            var chunk2 = t1.chunk(0, false, 4);
            assertEquals(4, chunk2.size());
            assertEquals(Shape.of(4, 13, 19, 17), chunk2.get(0).shape());
            assertEquals(Shape.of(4, 13, 19, 17), chunk2.get(1).shape());
            assertEquals(Shape.of(4, 13, 19, 17), chunk2.get(2).shape());
            assertEquals(Shape.of(13, 19, 17), chunk2.get(3).shape());

            var chunk3 = t1.chunk(2, true, 5);
            assertEquals(4, chunk3.size());
            assertEquals(Shape.of(13, 13, 5, 17), chunk3.get(0).shape());
            assertEquals(Shape.of(13, 13, 5, 17), chunk3.get(1).shape());
            assertEquals(Shape.of(13, 13, 5, 17), chunk3.get(2).shape());
            assertEquals(Shape.of(13, 13, 4, 17), chunk3.get(3).shape());

            var chunk4 = t1.chunkAll(true, new int[] {10, 10, 10, 10});
            assertEquals(16, chunk4.size());
            assertEquals(Shape.of(10, 10, 10, 10), chunk4.get(0).shape());
            assertEquals(Shape.of(10, 10, 10, 7), chunk4.get(1).shape());
            assertEquals(Shape.of(10, 10, 9, 10), chunk4.get(2).shape());
            assertEquals(Shape.of(10, 10, 9, 7), chunk4.get(3).shape());
            assertEquals(Shape.of(10, 3, 10, 10), chunk4.get(4).shape());
            assertEquals(Shape.of(10, 3, 10, 7), chunk4.get(5).shape());
            assertEquals(Shape.of(10, 3, 9, 10), chunk4.get(6).shape());
            assertEquals(Shape.of(10, 3, 9, 7), chunk4.get(7).shape());
            assertEquals(Shape.of(3, 10, 10, 10), chunk4.get(8).shape());
            assertEquals(Shape.of(3, 10, 10, 7), chunk4.get(9).shape());
            assertEquals(Shape.of(3, 10, 9, 10), chunk4.get(10).shape());
            assertEquals(Shape.of(3, 10, 9, 7), chunk4.get(11).shape());
            assertEquals(Shape.of(3, 3, 10, 10), chunk4.get(12).shape());
            assertEquals(Shape.of(3, 3, 10, 7), chunk4.get(13).shape());
            assertEquals(Shape.of(3, 3, 9, 10), chunk4.get(14).shape());
            assertEquals(Shape.of(3, 3, 9, 7), chunk4.get(15).shape());

            var c1 = g.mill().concat(3, chunk4.subList(0, 2));
            var c2 = g.mill().concat(3, chunk4.subList(2, 4));
            var c3 = g.mill().concat(3, chunk4.subList(4, 6));
            var c4 = g.mill().concat(3, chunk4.subList(6, 8));
            var c5 = g.mill().concat(3, chunk4.subList(8, 10));
            var c6 = g.mill().concat(3, chunk4.subList(10, 12));
            var c7 = g.mill().concat(3, chunk4.subList(12, 14));
            var c8 = g.mill().concat(3, chunk4.subList(14, 16));

            var c9 = g.mill().concat(2, List.of(c1, c2));
            var c10 = g.mill().concat(2, List.of(c3, c4));
            var c11 = g.mill().concat(2, List.of(c5, c6));
            var c12 = g.mill().concat(2, List.of(c7, c8));

            var c13 = g.mill().concat(1, List.of(c9, c10));
            var c14 = g.mill().concat(1, List.of(c11, c12));

            var c15 = g.mill().concat(0, List.of(c13, c14));

            assertTrue(t1.deepEquals(c15));
        }

        void repeatStackConcatTest() {

            var t1 = g.seq(Shape.of(10));

            var t2 = t1.repeat(0, 10, true);
            assertEquals(Shape.of(10, 10), t2.shape());

            var list1 = new ArrayList<T>();
            for (int i = 0; i < 10; i++) {
                list1.add(t1.unsqueeze(0));
            }
            assertTrue(t2.deepEquals(g.mill().concat(0, list1)));

            var t3 = t1.repeat(0, 10, false);
            assertEquals(Shape.of(10 * 10), t3.shape());
            var list2 = new ArrayList<T>();
            for (int i = 0; i < 10; i++) {
                list2.add(t1);
            }
            assertTrue(t3.deepEquals(g.mill().concat(0, list2)));
        }

        void aggregateOpsTest() {

            var t1 = g.seq(Shape.of(1, 200, 10));
            assertEquals(sequenceSum(t1.size()), t1.sum());
            assertEquals(g.value(1999), t1.max());
            assertEquals(g.value(1999), t1.nanMax());
            assertEquals(g.value(0), t1.min());
            assertEquals(g.value(0), t1.nanMin());

            var t2 = g.seq(Shape.of(79));
            assertEquals(sequenceSum(79), t2.sum());
            assertEquals(g.value(78), t2.max());
            assertEquals(g.value(78), t2.nanMax());
            assertEquals(g.value(0), t2.min());
            assertEquals(g.value(0), t2.nanMin());

            var t3 = g.seq(Shape.of(6)).narrow(0, true, 1, 6);
            assertEquals(g.value(120), t3.prod());
            assertEquals(g.value(5), t3.max());
            assertEquals(g.value(5), t3.nanMax());
            assertEquals(g.value(1), t3.min());
            assertEquals(g.value(1), t3.nanMin());

            var t4 = g.seq(Shape.of(101));
            t4.set(g.value(Double.NaN), 1);
            t4.set(g.value(Double.NaN), 100);
            assertEquals(g.sum(sequenceSum(100), g.value(-1)), t4.nanSum());
            if(g.dType().isFloat()) {
                assertEquals(2, t4.nanCount());
                assertEquals(1, t4.zeroCount());
                assertEquals(g.value(Double.NaN), t4.max());
                assertEquals(g.value(Double.NaN), t4.min());
            } else {
                assertEquals(0, t4.nanCount());
                assertEquals(3, t4.zeroCount());
                assertEquals(99, t4.max());
                assertEquals(0, t4.min());
            }
            assertEquals(g.value(99), t4.nanMax());
            assertEquals(g.value(0), t4.nanMin());


            var t5 = g.seq(Shape.of(7));
            t5.set(g.value(Double.NaN), 0);
            t5.set(g.value(Double.NaN), 6);
            if(g.dType().isFloat()) {
                assertEquals(2, t5.nanCount());
                assertEquals(0, t5.zeroCount());
                assertEquals(g.value(120), t5.nanProd());
                assertEquals(g.value(Double.NaN), t5.max());
                assertEquals(g.value(Double.NaN), t5.min());
                assertEquals(g.value(1), t5.nanMin());
            }else {
                assertEquals(0, t5.nanCount());
                assertEquals(2, t5.zeroCount());
                assertEquals(0, t5.nanProd());
                assertEquals(g.value(5), t5.max());
                assertEquals(g.value(0), t5.min());
                assertEquals(g.value(0), t5.nanMin());
            }
            assertEquals(g.value(5), t5.nanMax());

        }

        private N sequenceSum(int len) {
            N sum = g.value(0);
            for (int i = 1; i < len; i++) {
                sum = g.sum(sum, g.value(i));
            }
            return sum;
        }

        void statisticsTest() {

            if(!g.dType().isFloat()) {
                var e = assertThrows(IllegalArgumentException.class, () -> g.seq(Shape.of(10, 20)).stats());
                assertEquals("Operation available only for float tensors.", e.getMessage());
                return;
            }

            var t1 = g.seq(Shape.of(10, 20));
            var stat1 = t1.stats();
            assertEquals(g.value(99.5), stat1.mean());
            assertEquals(g.value(57.73430522661548), stat1.std());


            var t2 = g.seq(Shape.of(43, 43));
            var stat2 = t2.stats();

            double sum2 = (43*43-1)*(43*43)/2.;
            double size = 43*43;

            assertEquals(sum2, t2.sum().doubleValue());
            assertEquals(sum2 / size, stat2.mean().doubleValue(), 1e-5);

            var diff2 = t2.subNew(stat2.mean());
            var std2 = Math.sqrt(diff2.mulNew(diff2).divNew(g.value(t2.size())).sum().doubleValue());
            assertEquals(std2, stat2.std().doubleValue(), 1e-3);

            int k = 119;
            var t3 = g.random(Shape.of(k, k, k));
            Random random = new Random(42);
            for (int i = 0; i < k / 2; i++) {
                t3.set(g.value(Double.NaN), random.nextInt(k), random.nextInt(k), random.nextInt(k));
            }

            assertTrue(t3.nanCount() > 0);
            var stat3 = t3.stats();

            assertEquals(0.5, stat3.nanMean().doubleValue(), 1e-3);
            double mean3 = stat3.nanMean().doubleValue();
            double count3 = t3.size() - t3.nanCount();
            var diff3 = t3.subNew(g.value(mean3));
            double var3 = diff3.mulNew(diff3).nanSum().doubleValue() / count3;
            assertEquals(Math.sqrt(var3), stat3.nanStd().doubleValue(), 1e-3);

            assertEquals(t3.size() - t3.nanCount(), stat3.nanSize());
        }
    }

}
