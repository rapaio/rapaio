/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
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
 */

package rapaio.graphics.plot

import rapaio.data._
import rapaio.graphics.base._
import java.awt._

/**
 * @author tutuianu
 */
class Points(private val x: Feature, private var y: Feature) extends PlotComponent {

  def buildRange: Range = {
    if (math.min(x.rowCount, y.rowCount) == 0) null
    else {
      val range = new Range
      for (i <- 0 until math.min(x.rowCount, y.rowCount)) {
        if (!x.missing(i) && !y.missing(i)) range.union(x.values(i), y.values(i))
      }
      range
    }
  }

  def paint(g2d: Graphics2D) {
    g2d.setBackground(StandardColorPalette.color(255))
    for (i <- 0 until math.min(x.rowCount, y.rowCount)) {
      if (!x.missing(i) && !y.missing(i) && range.contains(x.values(i), y.values(i))) {
        g2d.setColor(options.col(i))
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f))
        val xx = parent.xScale(x.values(i)).toInt
        val yy = parent.yScale(y.values(i)).toInt
        PchPalette.draw(g2d, xx, yy, options.sz(i), options.pch(i))
      }
    }
  }
}