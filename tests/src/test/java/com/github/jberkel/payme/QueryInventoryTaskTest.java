package com.github.jberkel.payme;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import static com.github.jberkel.payme.QueryInventoryTask.QueryArgs;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class QueryInventoryTaskTest {
    @Mock IabHelper iabHelper;
    @Mock QueryInventoryFinishedListener listener;

    QueryInventoryTask task;

    @Before public void before() throws Exception {
        initMocks(this);
        task = new QueryInventoryTask(iabHelper, listener);
    }

    @Test public void shouldQueryInventory() throws Exception {
        Inventory inventory = mock(Inventory.class);
        when(iabHelper.queryInventory(false, null)).thenReturn(inventory);
        Inventory result = task.execute(new QueryArgs(false, null)).get();
        assertThat(result).isSameAs(inventory);
        verify(listener).onQueryInventoryFinished(new IabResult(Response.OK), inventory);
    }

    @Test public void shouldQueryInventoryWithException() throws Exception {
        when(iabHelper.queryInventory(false, null)).thenThrow(new IabException(Response.ERROR, ""));
        Inventory result = task.execute(new QueryArgs(false, null)).get();
        assertThat(result).isNull();
        verify(listener).onQueryInventoryFinished(new IabResult(Response.ERROR), null);
    }

    @Test public void shouldNotNotifyListenerIfHelperWasDisposed() throws Exception {
        Inventory inventory = mock(Inventory.class);
        when(iabHelper.queryInventory(false, null)).thenReturn(inventory);
        when(iabHelper.isDisposed()).thenReturn(true);
        task.execute(new QueryArgs(false, null)).get();
        verifyZeroInteractions(listener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckArgumentsNull() throws Exception {
        task.doInBackground((QueryArgs) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckArgumentsEmpty() throws Exception {
        task.doInBackground(new QueryArgs[0]);
    }
}
