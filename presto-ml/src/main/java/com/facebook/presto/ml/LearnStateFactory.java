/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.ml;

import com.facebook.presto.operator.aggregation.state.AbstractGroupedAccumulatorState;
import com.facebook.presto.operator.aggregation.state.AccumulatorStateFactory;
import com.facebook.presto.util.array.ObjectBigArray;
import libsvm.svm_parameter;
import org.openjdk.jol.info.ClassLayout;

import java.util.ArrayList;
import java.util.List;

public class LearnStateFactory
        implements AccumulatorStateFactory<LearnState>
{
    private static final long ARRAY_LIST_SIZE = ClassLayout.parseClass(ArrayList.class).instanceSize();
    private static final long SVM_PARAMETERS_SIZE = ClassLayout.parseClass(svm_parameter.class).instanceSize();

    @Override
    public LearnState createSingleState()
    {
        return new SingleLearnState();
    }

    @Override
    public Class<? extends LearnState> getSingleStateClass()
    {
        return SingleLearnState.class;
    }

    @Override
    public LearnState createGroupedState()
    {
        return new GroupedLearnState();
    }

    @Override
    public Class<? extends LearnState> getGroupedStateClass()
    {
        return GroupedLearnState.class;
    }

    public static class GroupedLearnState
            extends AbstractGroupedAccumulatorState
            implements LearnState
    {
        private final ObjectBigArray<List<Double>> labelsArray = new ObjectBigArray<>();
        private final ObjectBigArray<List<FeatureVector>> featureVectorsArray = new ObjectBigArray<>();
        private final ObjectBigArray<svm_parameter> parametersArray = new ObjectBigArray<>();
        private long size;

        @Override
        public void ensureCapacity(long size)
        {
            labelsArray.ensureCapacity(size);
            featureVectorsArray.ensureCapacity(size);
            parametersArray.ensureCapacity(size);
        }

        @Override
        public long getEstimatedSize()
        {
            return size + labelsArray.sizeOf() + featureVectorsArray.sizeOf();
        }

        @Override
        public List<Double> getLabels()
        {
            List<Double> labels = labelsArray.get(getGroupId());
            if (labels == null) {
                labels = new ArrayList<>();
                size += ARRAY_LIST_SIZE;
                // Assume that one parameter will be set for each group of labels
                size += SVM_PARAMETERS_SIZE;
                labelsArray.set(getGroupId(), labels);
            }
            return labels;
        }

        @Override
        public List<FeatureVector> getFeatureVectors()
        {
            List<FeatureVector> featureVectors = featureVectorsArray.get(getGroupId());
            if (featureVectors == null) {
                featureVectors = new ArrayList<>();
                size += ARRAY_LIST_SIZE;
                featureVectorsArray.set(getGroupId(), featureVectors);
            }
            return featureVectors;
        }

        @Override
        public svm_parameter getParameters()
        {
            return parametersArray.get(getGroupId());
        }

        @Override
        public void setParameters(svm_parameter parameters)
        {
            parametersArray.set(getGroupId(), parameters);
        }

        @Override
        public void addMemoryUsage(long value)
        {
            size += value;
        }
    }

    public static class SingleLearnState
            implements LearnState
    {
        private final List<Double> labels = new ArrayList<>();
        private final List<FeatureVector> featureVectors = new ArrayList<>();
        private svm_parameter parameters;
        private long size;

        @Override
        public long getEstimatedSize()
        {
            return size + 2 * ARRAY_LIST_SIZE;
        }

        @Override
        public List<Double> getLabels()
        {
            return labels;
        }

        @Override
        public List<FeatureVector> getFeatureVectors()
        {
            return featureVectors;
        }

        @Override
        public svm_parameter getParameters()
        {
            return parameters;
        }

        @Override
        public void setParameters(svm_parameter parameters)
        {
            this.parameters = parameters;
        }

        @Override
        public void addMemoryUsage(long value)
        {
            size += value;
        }
    }
}