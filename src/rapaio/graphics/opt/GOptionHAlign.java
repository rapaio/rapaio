/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 - 2021 Aurelian Tutuianu
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

package rapaio.graphics.opt;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 9/14/17.
 */
public class GOptionHAlign implements GOption<Integer> {

    private static final long serialVersionUID = -4310646137630324226L;
    private final int hAlign;

    public GOptionHAlign(int hAlign) {
        this.hAlign = hAlign;
    }

    @Override
    public void bind(GOptions opts) {
        opts.setHAlign(this);
    }

    @Override
    public Integer apply(GOptions opts) {
        return hAlign;
    }
}