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

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.PreparedRead;
import org.eclipse.kura.gpio.GPIOService;
import org.eclipse.kura.gpio.KuraClosedDeviceException;
import org.eclipse.kura.gpio.KuraGPIODescription;
import org.eclipse.kura.gpio.KuraGPIODeviceException;
import org.eclipse.kura.gpio.KuraGPIODirection;
import org.eclipse.kura.gpio.KuraGPIOMode;
import org.eclipse.kura.gpio.KuraGPIOPin;
import org.eclipse.kura.gpio.KuraGPIOTrigger;
import org.eclipse.kura.gpio.KuraUnavailableDeviceException;
import org.eclipse.kura.gpio.PinStatusListener;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GPIODriverTest {

	@InjectMocks
	private GPIODriver driver;
	@Mock
	private GPIOService gpioService;
	@Mock
	private KuraGPIOPin gpioPin;
	@Mock
	private ChannelListener listener;
	private Map<String, Object> config;
	private ChannelRecord channelRecord;
	private ChannelDescriptor channelDescriptor;
	private PreparedRead preparedRead;

	@Test
	public void shouldActivateDriver() {
		givenBindGPIOService();

		whenDriverIsActivated(Collections.emptyMap());

		thenDriverIsNotNull();
		thenNoInteractionsWithGPIOService();
	}

	@Test
	public void shouldUpdateDriver() {
		givenBindGPIOService();

		whenDriverIsUpdated(Collections.emptyMap());

		thenDriverIsNotNull();
		thenNoInteractionsWithGPIOService();
	}

	@Test
	public void shouldDeactivateDriver()
			throws ConnectionException, IOException, KuraUnavailableDeviceException, KuraClosedDeviceException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenInputChannelRecord(this.gpioPin.getDescription().getDisplayName(), DataType.BOOLEAN, true);

		whenDriverIsActivated(Collections.emptyMap());
		whenReadOperationIsInvoked();
		whenDriverIsDeactivated();

		thenDriverIsNotNull();
		thenGPIOServiceGetPinsIsCalled(1, 1, 1, this.gpioPin.getDescription().getProperties().get("name"),
				KuraGPIODirection.INPUT,
				KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		thenGpioPinIsClosed();
	}

	@Test
	public void shouldDoNothingWhenConnectIsInvoked() throws Exception {
		givenBindGPIOService();

		whenDriverIsActivated(Collections.emptyMap());
		whenDriverIsConnected();

		thenDriverIsNotNull();
		thenNoInteractionsWithGPIOService();
	}

	@Test
	public void shouldDisconnect()
			throws ConnectionException, IOException, KuraUnavailableDeviceException, KuraClosedDeviceException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenInputChannelRecord(this.gpioPin.getDescription().getDisplayName(), DataType.BOOLEAN, true);

		whenDriverIsActivated(Collections.emptyMap());
		whenReadOperationIsInvoked();
		whenDriverIsDisconnected();

		thenDriverIsNotNull();
		thenGPIOServiceGetPinsIsCalled(1, 1, 1, this.gpioPin.getDescription().getProperties().get("name"),
				KuraGPIODirection.INPUT,
				KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		thenGpioPinIsClosed();
	}

	@Test
	public void shouldGetChannelDescriptor() {
		givenBindGPIOService();

		whenDriverIsActivated(Collections.emptyMap());
		whenChannelDescriptorIsRetrieved();

		thenDriverIsNotNull();
		thenChannelDescriptorIsNotNull();
	}

	@Test
	public void shouldWriteBooleanValueToOutputPin() throws Exception {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN,
				KuraGPIOTrigger.NONE);
		givenOutputChannelRecord(this.gpioPin.getDescription().getDisplayName(), true);

		whenDriverIsActivated(Collections.emptyMap());
		whenWriteOperationIsInvoked();

		thenPinValueIsSetAndRecordMarkedSuccess();
	}

	@Test
	public void shouldMarkFailureWhenWriteHasMissingConfig() throws Exception {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN,
				KuraGPIOTrigger.NONE);
		givenChannelRecordWithDefaultProperties();

		whenDriverIsActivated(Collections.emptyMap());
		whenWriteOperationIsInvoked();

		thenRecordMarkedFailure();
	}

	@Test
	public void shouldReadValueAndPopulateChannelRecord() throws KuraUnavailableDeviceException,
			KuraClosedDeviceException, IOException, ConnectionException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenInputChannelRecord(this.gpioPin.getDescription().getDisplayName(), DataType.BOOLEAN, true);

		whenDriverIsActivated(Collections.emptyMap());
		whenReadOperationIsInvoked();

		thenChannelRecordReceivesValue(true);
	}

	@Test
	public void shouldRegisterChannelListener() throws ConnectionException, KuraClosedDeviceException, IOException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenListenerConfig(this.gpioPin.getDescription().getDisplayName(), KuraGPIODirection.INPUT);

		whenDriverIsActivated(Collections.emptyMap());
		whenRegisterListenerIsInvoked();

		thenPinListenerIsInstalledAndTriggered(listener);
	}

	@Test
	public void shouldUnregisterChannelListener() throws ConnectionException, KuraClosedDeviceException, IOException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenListenerConfig(this.gpioPin.getDescription().getDisplayName(), KuraGPIODirection.INPUT);

		whenDriverIsActivated(Collections.emptyMap());
		whenRegisterListenerIsInvoked();
		whenUnregisterListenerIsInvoked();

		thenPinListenerIsRemoved();
	}

	@Test
	public void shouldDisconnectAndCleanupResources() throws Exception {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenListenerConfig(this.gpioPin.getDescription().getDisplayName(), KuraGPIODirection.INPUT);

		whenDriverIsActivated(Collections.emptyMap());
		whenRegisterListenerIsInvoked();
		whenDisconnectIsInvoked();

		thenPinListenerIsRemoved();
		thenPinIsClosed();
	}

	@Test
	public void shouldPrepareReadAndExecuteSuccessfully() throws IOException, ConnectionException, KuraException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenInputChannelRecord(this.gpioPin.getDescription().getDisplayName(), DataType.BOOLEAN, true);

		whenDriverIsActivated(Collections.emptyMap());
		whenPreparedReadIsCreated();
		whenPrepareReadIsExecuted();

		thenChannelRecordReceivesValue(true);
	}

	@Test
	public void shouldFailWriteWhenPinSetThrowsException()
			throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException, ConnectionException {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.OUTPUT, KuraGPIOMode.OUTPUT_OPEN_DRAIN,
				KuraGPIOTrigger.NONE);
		givenOutputChannelRecord(this.gpioPin.getDescription().getDisplayName(), true);
		givenExceptionOnSetValue();

		whenDriverIsActivated(Collections.emptyMap());
		whenWriteOperationIsInvoked();

		thenRecordMarkedFailure();
	}

	@Test
	public void shouldFailReadWhenPinThrows() throws Exception {
		givenBindGPIOService();
		givenGPIOPin("AwesomePin", 1, 1, KuraGPIODirection.INPUT, KuraGPIOMode.INPUT_PULL_UP, KuraGPIOTrigger.NONE);
		givenInputChannelRecord(this.gpioPin.getDescription().getDisplayName(), DataType.BOOLEAN, true);
		givenExceptionOnGetValue();

		whenDriverIsActivated(Collections.emptyMap());
		whenReadOperationIsInvoked();

		thenRecordMarkedFailure();
	}

	/**
	 * Given
	 */

	private void givenBindGPIOService() {
		this.driver.bindGPIOService(this.gpioService);
	}

	private void givenGPIOPin(String pinName, int controller, int line, KuraGPIODirection resourceDirection,
			KuraGPIOMode resourceMode, KuraGPIOTrigger resourceTrigger) {
		Map<String, String> properties = new java.util.HashMap<>();
		properties.put("controller", Integer.toString(controller));
		properties.put("line", Integer.toString(line));
		properties.put("name", pinName);
		properties.put(KuraGPIODescription.DISPLAY_NAME_PROPERTY, pinName + ":" + controller + ":" + line);
		KuraGPIODescription description = new KuraGPIODescription(properties);
		when(this.gpioService.getPins(properties, resourceDirection, resourceMode, resourceTrigger))
				.thenReturn(Arrays.asList(this.gpioPin));
		when(this.gpioService.getPins(properties)).thenReturn(Arrays.asList(this.gpioPin));
		when(this.gpioPin.getDescription()).thenReturn(description);
		when(gpioPin.isOpen()).thenReturn(true);
		when(this.gpioService.getAvailablePinDescriptions())
				.thenReturn(Arrays.asList(description));
	}

	private void givenExceptionOnSetValue() throws KuraUnavailableDeviceException,
			KuraClosedDeviceException, IOException {
		doThrow(new IOException("boom")).when(this.gpioPin).setValue(true);
	}

	private void givenExceptionOnGetValue() throws KuraUnavailableDeviceException,
			KuraClosedDeviceException, IOException {
		doThrow(new IOException("boom")).when(this.gpioPin).getValue();
	}

	private void givenOutputChannelRecord(String pinName, boolean value) {
		this.channelRecord = ChannelRecord.createWriteRecord(pinName, TypedValues.newBooleanValue(value));
		this.config = new HashMap<>();
		this.config.put("resource.name", pinName);
		this.config.put("resource.direction", KuraGPIODirection.OUTPUT.name());
		this.config.put("resource.mode", KuraGPIOMode.OUTPUT_OPEN_DRAIN.name());
		this.config.put("resource.trigger", KuraGPIOTrigger.NONE.name());
		this.channelRecord.setChannelConfig(config);
	}

	private void givenChannelRecordWithDefaultProperties() {
		this.channelRecord = ChannelRecord.createWriteRecord("PIN-MISSING",
				TypedValues.newBooleanValue(true));
		this.config = new HashMap<>();
		this.config.put("resource.name", GPIOChannelDescriptor.DEFAULT_RESOURCE_NAME);
		this.config.put("resource.direction", GPIOChannelDescriptor.DEFAULT_RESOURCE_DIRECTION);
		this.config.put("resource.mode", GPIOChannelDescriptor.DEFAULT_RESOURCE_MODE);
		this.config.put("resource.trigger", KuraGPIOTrigger.NONE.name());
		channelRecord.setChannelConfig(config);
	}

	private void givenInputChannelRecord(String pinName, DataType type, boolean value)
			throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException {
		this.channelRecord = ChannelRecord.createReadRecord(pinName, type);
		this.config = new HashMap<>();
		this.config.put("resource.name", pinName);
		this.config.put("resource.direction", KuraGPIODirection.INPUT.name());
		this.config.put("resource.mode", KuraGPIOMode.INPUT_PULL_UP.name());
		this.config.put("resource.trigger", KuraGPIOTrigger.NONE.name());
		channelRecord.setChannelConfig(config);

		when(gpioPin.getValue()).thenReturn(value);
	}

	private void givenListenerConfig(String pinName, KuraGPIODirection direction) {
		this.config = new HashMap<>();
		this.config.put("resource.name", pinName);
		this.config.put("resource.direction", direction.name());
		this.config.put("resource.mode", KuraGPIOMode.INPUT_PULL_UP.name());
		this.config.put("resource.trigger", KuraGPIOTrigger.NONE.name());
		this.config.put("+name", "listener-channel");
		this.config.put("+value.type", DataType.BOOLEAN.name());
	}

	/**
	 * When
	 */

	private void whenDriverIsActivated(Map<String, Object> config) {
		this.driver.activate(config);
	}

	private void whenDriverIsUpdated(Map<String, Object> config) {
		this.driver.update(config);
	}

	private void whenDriverIsDeactivated() {
		this.driver.deactivate();
	}

	private void whenDriverIsConnected() throws ConnectionException {
		this.driver.connect();
	}

	private void whenDriverIsDisconnected() throws ConnectionException {
		this.driver.disconnect();
	}

	private void whenChannelDescriptorIsRetrieved() {
		this.channelDescriptor = this.driver.getChannelDescriptor();
	}

	private void whenWriteOperationIsInvoked() throws ConnectionException {
		this.driver.write(singletonList(this.channelRecord));
	}

	private void whenReadOperationIsInvoked() throws ConnectionException {
		this.driver.read(singletonList(this.channelRecord));
	}

	private final void whenPrepareReadIsExecuted() throws ConnectionException, KuraException {
		this.preparedRead.execute();
	}

	private void whenRegisterListenerIsInvoked()
			throws ConnectionException {
		driver.registerChannelListener(this.config, this.listener);
	}

	private void whenUnregisterListenerIsInvoked() throws ConnectionException {
		driver.unregisterChannelListener(this.listener);
	}

	private void whenDisconnectIsInvoked() throws ConnectionException {
		driver.disconnect();
	}

	private void whenPreparedReadIsCreated() {
		this.preparedRead = this.driver.prepareRead(singletonList(this.channelRecord));
	}

	/**
	 * Then
	 */

	private void thenDriverIsNotNull() {
		assertNotNull(this.driver);
	}

	private void thenNoInteractionsWithGPIOService() {
		verify(this.gpioService, never()).getPins(any(Map.class), any(KuraGPIODirection.class),
				any(KuraGPIOMode.class), any(KuraGPIOTrigger.class));
	}

	private void thenGPIOServiceGetPinsIsCalled(int times, int controller, int line, String name,
			KuraGPIODirection direction,
			KuraGPIOMode mode, KuraGPIOTrigger trigger) {
		Map<String, String> properties = new java.util.HashMap<>();
		properties.put("controller", Integer.toString(controller));
		properties.put("line", Integer.toString(line));
		properties.put("name", name);
		properties.put(KuraGPIODescription.DISPLAY_NAME_PROPERTY, name + ":" + controller + ":" + line);
		verify(this.gpioService, org.mockito.Mockito.times(times)).getPins(properties, direction, mode, trigger);
	}

	private void thenGpioPinIsClosed() throws IOException {
		verify(this.gpioPin, atLeastOnce()).close();
	}

	private void thenChannelDescriptorIsNotNull() {
		assertNotNull(this.channelDescriptor);
	}

	private void thenPinValueIsSetAndRecordMarkedSuccess()
			throws KuraUnavailableDeviceException, KuraClosedDeviceException, IOException, KuraGPIODeviceException {
		verify(gpioPin).setValue(true);
		verify(gpioPin, never()).open();
		verify(gpioPin, never()).close();

		ChannelStatus status = this.channelRecord.getChannelStatus();
		assertNotNull(status);
		assertEquals(ChannelFlag.SUCCESS, status.getChannelFlag());
	}

	private void thenRecordMarkedFailure() {
		ChannelStatus status = this.channelRecord.getChannelStatus();
		assertNotNull(status);
		assertEquals(ChannelFlag.FAILURE, status.getChannelFlag());
	}

	private void thenChannelRecordReceivesValue(boolean expectedValue) {
		ChannelStatus status = this.channelRecord.getChannelStatus();
		assertNotNull(status);
		assertEquals(ChannelFlag.SUCCESS, status.getChannelFlag());
		assertNotNull(this.channelRecord.getValue());
		assertEquals(expectedValue, (Boolean) this.channelRecord.getValue().getValue());
	}

	private void thenPinListenerIsInstalledAndTriggered(ChannelListener listener)
			throws KuraClosedDeviceException, IOException {
		ArgumentCaptor<PinStatusListener> captor = ArgumentCaptor.forClass(PinStatusListener.class);
		verify(gpioPin).addPinStatusListener(captor.capture());

		PinStatusListener pinStatusListener = captor.getValue();
		pinStatusListener.pinStatusChange(true);
		verify(listener).onChannelEvent(any(ChannelEvent.class));
	}

	private void thenPinListenerIsRemoved() throws KuraClosedDeviceException, IOException {
		verify(this.gpioPin, atLeastOnce()).removePinStatusListener(any(PinStatusListener.class));
	}

	private void thenPinIsClosed() throws IOException {
		verify(this.gpioPin, atLeastOnce()).close();
	}
}
