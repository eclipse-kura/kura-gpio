package org.eclipse.kura.linux.gpio.libgpiod;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.gpio.KuraGPIODescription;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.junit.Before;
import org.junit.Test;

public class GPIOServiceFilterTest {

    private static final String NAME_PIN11 = "pin11";
    private static final String NAME_PIN12 = "pin12";
    private static final int CTRL_11 = 11;
    private static final int CTRL_12 = 12;
    private static final int LINE_11 = 11;
    private static final int LINE_12 = 12;

    private StubLibGpiodService service;
    private Map<String, String> description;;
    private List<KuraGPIOPin> pins;

    @Before
    public void setUp() {
        this.service = new StubLibGpiodService();
        this.service.preload(
                newDescription(NAME_PIN11, CTRL_11, LINE_11),
                newDescription(NAME_PIN11, CTRL_12, LINE_12),
                newDescription(NAME_PIN12, CTRL_11, LINE_11),
                newDescription(NAME_PIN11, null, null));
    }

    @Test
    public void shouldFilterByNameOnly() {
        givenPinDescription(NAME_PIN11, null, null);

        whenRequestingPins();

        thenPinsCountIs(pins, 3);
    }

    @Test
    public void shouldFilterByNameAndController() {
        givenPinDescription(NAME_PIN11, CTRL_11, null);

        whenRequestingPins();

        thenPinsCountIs(pins, 1);
    }

    @Test
    public void shouldFilterByControllerOnly() {
        givenPinDescription(null, CTRL_11, null);

        whenRequestingPins();

        thenPinsCountIs(pins, 2);
    }

    @Test
    public void shouldFilterByLineOnly() {
        givenPinDescription(null, null, LINE_11);

        whenRequestingPins();

        thenPinsCountIs(pins, 2);
    }

    @Test
    public void shouldFilterByNameAndLine() {
        givenPinDescription(NAME_PIN11, null, LINE_11);

        whenRequestingPins();

        thenPinsCountIs(pins, 1);
    }

    @Test
    public void shouldFilterByNameControllerAndLine() {
        givenPinDescription(NAME_PIN11, CTRL_11, LINE_11);

        whenRequestingPins();

        thenPinsCountIs(pins, 1);
    }

    private void givenPinDescription(String name, Integer controller, Integer line) {
        this.description = new HashMap<>();
        if (name != null) {
            description.put(LibGpiodGPIOService.GPIO_NAME, name);
        }
        if (controller != null) {
            description.put(LibGpiodGPIOService.GPIO_CONTROLLER, Integer.toString(controller));
        }
        if (line != null) {
            description.put(LibGpiodGPIOService.GPIO_LINE, Integer.toString(line));
        }
    }

    private void whenRequestingPins() {
        this.pins = this.service.getPins(this.description);
    }

    private void thenPinsCountIs(List<KuraGPIOPin> pins, int expected) {
        assertEquals(expected, pins.size());
    }

    private KuraGPIODescription newDescription(String name, Integer controller, Integer line) {
        Map<String, String> props = new HashMap<>();
        if (name != null) {
            props.put(LibGpiodGPIOService.GPIO_NAME, name);
        }
        if (controller != null) {
            props.put(LibGpiodGPIOService.GPIO_CONTROLLER, Integer.toString(controller));
        }
        if (line != null) {
            props.put(LibGpiodGPIOService.GPIO_LINE, Integer.toString(line));
        }
        props.put(KuraGPIODescription.DISPLAY_NAME_PROPERTY,
                String.format("%s:%s:%s", name, controller, line));
        return new KuraGPIODescription(props);
    }

    private static class StubLibGpiodService extends LibGpiodGPIOService {

        @Override
        public void initialize() {
            this.initialized.set(true);
        }

        void preload(KuraGPIODescription... descriptions) {
            this.availablePinDescriptions.clear();
            this.availablePinDescriptions.addAll(Arrays.asList(descriptions));
            this.initialized.set(true);
        }

        @Override
        protected void discoverChipPins(String chipPath) {
            // no-op for tests
        }

        @Override
        protected boolean isValidPin(String chipPath, int offset) {
            return true;
        }

        @Override
        protected KuraGPIOPin createPin(KuraGPIODescription description, KuraGPIODirection direction,
                KuraGPIOMode mode, KuraGPIOTrigger trigger) {
            return mock(KuraGPIOPin.class);
        }
    }

}
