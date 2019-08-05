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

package rapaio.data.group.function;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import rapaio.data.*;
import rapaio.data.group.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/20/19.
 */
public abstract class DefaultSingleGroupFun extends DefaultGroupFun {

    protected static final String SEPARATOR = "_";

    protected final int normalizeLevel;

    public DefaultSingleGroupFun(String name, int normalizeLevel, List<String> varNames) {
        super(name, varNames);
        this.normalizeLevel = normalizeLevel;
    }

    public abstract Var buildVar(Group group, String varName);

    public abstract void updateSingle(Var aggregate, int aggregateRow, Frame df, int varIndex, IntList rows);

    @Override
    public List<Var> compute(Group group) {
        List<Var> result = new ArrayList<>();
        IntList ids = group.getSortedGroupIds();
        for (String varName : varNames) {
            Var aggregate = buildVar(group, varName);
            int index = group.getFrame().varIndex(varName);
            for (int i = 0; i < ids.size(); i++) {
                int groupId = ids.getInt(i);
                updateSingle(aggregate, i, group.getFrame(), index, group.getRowsForGroupId(groupId));
            }
            if (normalizeLevel < 0) {
                result.add(aggregate);
                continue;
            }
            if (normalizeLevel == 0) {
                result.add(VarBinary.fill(aggregate.rowCount(), 1).withName(aggregate.name()+"_N0"));
                continue;
            }
            result.add(normalize(group, aggregate));
        }
        return result;
    }

    private Var normalize(Group group, Var agg) {
        int count = group.getGroupCount();

        Int2ObjectOpenHashMap<Group.IndexNode> groupIndex = group.getGroupIdToLastLevelIndex();

        Int2ObjectOpenHashMap<Group.IndexNode> reducedGroup = new Int2ObjectOpenHashMap<>();
        Object2DoubleOpenHashMap<Group.IndexNode> sum = new Object2DoubleOpenHashMap<>();
        for (int i = 0; i < count; i++) {
            Group.IndexNode node = groupIndex.get(i);
            for (int j = 0; j < normalizeLevel; j++) {
                node = node.getParent();
            }
            reducedGroup.put(i, node);
            sum.put(node, 0);
        }

        // accumulate at higher group

        for (int i = 0; i < group.getGroupCount(); i++) {
            double value = agg.getDouble(i);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                continue;
            }
            Group.IndexNode node = reducedGroup.get(i);
            double oldSum = sum.getDouble(node);
            sum.put(node, oldSum + value);
        }

        // normalize

        VarDouble normalized = VarDouble.empty(count).withName(agg.name() + "_N" + normalizeLevel);
        for (int i = 0; i < group.getGroupCount(); i++) {
            double value = agg.getDouble(i);
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                continue;
            }
            Group.IndexNode node = reducedGroup.get(i);
            double groupSum = sum.getDouble(node);
            if (Double.isNaN(groupSum) || Double.isInfinite(groupSum) || groupSum == 0) {
                continue;
            }
            normalized.setDouble(i, value / groupSum);
        }
        return normalized;
    }
}