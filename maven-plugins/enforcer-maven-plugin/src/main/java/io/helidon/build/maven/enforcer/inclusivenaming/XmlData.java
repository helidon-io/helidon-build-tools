/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
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

package io.helidon.build.maven.enforcer.inclusivenaming;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * XML Data.
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlData {

    @XmlElement(name = "recommended_replacements")
    @Parameter
    private String[] recommendedReplacements = new String[0];

    @Parameter
    private String tier = "";

    @Parameter
    private String recommendation = "";

    @Parameter
    private String term = "";

    @XmlElement(name = "term_page")
    @Parameter
    private String termPage = "";

    /**
     * Default constructor.
     */
    public XmlData() {
    }

    /**
     * Get the recommendedReplacements.
     *
     * @return the recommendedReplacements
     */
    public List<String> getRecommendedReplacements() {
        return Arrays.asList(recommendedReplacements);
    }

    /**
     * Set the recommendedReplacements.
     *
     * @param recommendedReplacements
     */
    public void setRecommendedReplacements(String[] recommendedReplacements) {
        this.recommendedReplacements = recommendedReplacements;
    }

    /**
     * Get the tier.
     *
     * @return the tier
     */
    public String getTier() {
        return tier;
    }

    /**
     * Set the tier.
     *
     * @param tier
     */
    public void setTier(String tier) {
        this.tier = tier;
    }

    /**
     * Get the recommendation.
     *
     * @return the recommendation
     */
    public String getRecommendation() {
        return recommendation;
    }

    /**
     * Set the recommendation.
     *
     * @param recommendation
     */
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    /**
     * Get the term.
     *
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * Set the term.
     *
     * @param term
     */
    public void setTerm(String term) {
        this.term = term;
    }

    /**
     * Get the termPage.
     *
     * @return the termPage
     */
    public String getTermPage() {
        return termPage;
    }

    /**
     * Set the termPage.
     *
     * @param termPage
     */
    public void setTermPage(String termPage) {
        this.termPage = termPage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{").append(term).append(": ").append(recommendation)
        .append(" Recommended replacements").append(Arrays.asList(recommendedReplacements)).append("}");
        return builder.toString();
    }
}
