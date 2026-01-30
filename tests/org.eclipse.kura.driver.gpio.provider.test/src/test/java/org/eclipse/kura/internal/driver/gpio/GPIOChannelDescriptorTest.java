/**
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 */
package org.eclipse.kura.internal.driver.gpio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraGPIODescription;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GPIOChannelDescriptorTest {

    @Mock
    private GPIOService gpioService;
    private GPIOChannelDescriptor descriptor;
    private List<Tad> elements;
    private Map<String, Object> config;
    private KuraGPIODirection direction;
    private KuraGPIOTrigger trigger;

    @Before
    public void setUp() {
        getAvailablePinDescriptions("PIN1", "PIN2");
        givenDescriptor();
    }

    @Test
    public void shouldBuildDescriptorWithPinsDefaultsAndEnums() {
        whenDescriptorIsBuilt();

        thenResourceNameOptionsContain(elements, "PIN1:0:0", "PIN2:1:1", GPIOChannelDescriptor.DEFAULT_RESOURCE_NAME);
        thenDirectionOptionsContain(elements, GPIOChannelDescriptor.DEFAULT_RESOURCE_DIRECTION);
        thenTriggerOptionsContain(elements);
    }

    @Test
    public void shouldReturnNullForDefaultDirection() {
        givenDirectionConfig(GPIOChannelDescriptor.DEFAULT_RESOURCE_DIRECTION);

        whenDirectionIsParsed();

        thenDirectionIsNull();
    }

    @Test
    public void shouldParseConcreteDirection() {
        givenDirectionConfig(KuraGPIODirection.INPUT.name());

        whenDirectionIsParsed();

        thenDirectionMatches(KuraGPIODirection.INPUT);
    }

    @Test
    public void shouldParseTrigger() {
        givenTriggerConfig(KuraGPIOTrigger.BOTH_EDGES.name());

        whenTriggerIsParsed();

        thenTriggerMatches(KuraGPIOTrigger.BOTH_EDGES);
    }

    /**
     * Given
     */

    private void givenDescriptor() {
        this.descriptor = new GPIOChannelDescriptor(Arrays.asList(this.gpioService));
    }

    private void getAvailablePinDescriptions(String... pins) {
        List<KuraGPIODescription> descriptions = new ArrayList<>();
        for (int i = 0; i < pins.length; i++) {
            descriptions.add(new KuraGPIODescription(i, i, pins[i]));
        }
        when(gpioService.getAvailablePinDescriptions()).thenReturn(descriptions);
    }

    private void givenDirectionConfig(String directionValue) {
        this.config = new HashMap<>();
        this.config.put("resource.direction", directionValue);
    }

    private void givenTriggerConfig(String triggerValue) {
        this.config = new HashMap<>();
        this.config.put("resource.trigger", triggerValue);
    }

    /**
     * When
     */

    @SuppressWarnings("unchecked")
    private void whenDescriptorIsBuilt() {
        Object descriptorObj = descriptor.getDescriptor();
        this.elements = new ArrayList<>((List<Tad>) descriptorObj);
    }

    private void whenDirectionIsParsed() {
        this.direction = GPIOChannelDescriptor.getResourceDirection(this.config);
    }

    private void whenTriggerIsParsed() {
        this.trigger = GPIOChannelDescriptor.getResourceTrigger(this.config);
    }

    /**
     * Then
     */

    private void thenResourceNameOptionsContain(List<Tad> elements, String... expectedValues) {
        List<String> values = extractOptionValues(elements, "resource.name");
        for (String expected : expectedValues) {
            assertTrue(values.contains(expected));
        }
    }

    private void thenDirectionOptionsContain(List<Tad> elements, String defaultValue) {
        List<String> values = extractOptionValues(elements, "resource.direction");
        for (KuraGPIODirection gpioDirection : KuraGPIODirection.values()) {
            assertTrue(values.contains(gpioDirection.name()));
        }
        assertTrue(values.contains(defaultValue));
    }

    private void thenTriggerOptionsContain(List<Tad> elements) {
        List<String> values = extractOptionValues(elements, "resource.trigger");
        for (KuraGPIOTrigger gpioTrigger : KuraGPIOTrigger.values()) {
            assertTrue(values.contains(gpioTrigger.name()));
        }
    }

    private void thenDirectionIsNull() {
        assertNull(direction);
    }

    private void thenDirectionMatches(KuraGPIODirection expected) {
        assertEquals(expected, this.direction);
    }

    private void thenTriggerMatches(KuraGPIOTrigger expected) {
        assertEquals(expected, this.trigger);
    }

    private List<String> extractOptionValues(List<Tad> elements, String id) {
        Tad element = elements.stream().filter(t -> id.equals(t.getId())).findFirst().orElse(null);
        return element.getOption().stream().map(Option::getValue).collect(Collectors.toList());
    }
}
