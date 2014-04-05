package com.github.jberkel.pay.me;

import com.github.jberkel.pay.me.listener.QueryInventoryFinishedListener;
import com.github.jberkel.pay.me.model.Inventory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class QueryInventoryTaskTest {
    @Mock
    IabHelper iabHelper;
    @Mock
    QueryInventoryFinishedListener listener;

    QueryInventoryTask task;

    @Before public void before() throws Exception {
        initMocks(this);
        task = new QueryInventoryTask(iabHelper, listener);
    }

    @Test public void shouldQueryInventory() throws Exception {
        Inventory inventory = mock(Inventory.class);
        when(iabHelper.queryInventory(false, null, null)).thenReturn(inventory);
        Inventory result = task.execute(new QueryInventoryTask.Args(false, null, null)).get();
        assertThat(result).isSameAs(inventory);
        verify(listener).onQueryInventoryFinished(new IabResult(Response.OK), inventory);
    }

    @Test public void shouldQueryInventoryWithException() throws Exception {
        when(iabHelper.queryInventory(false, null, null)).thenThrow(new IabException(Response.ERROR, ""));
        Inventory result = task.execute(new QueryInventoryTask.Args(false, null, null)).get();
        assertThat(result).isNull();
        verify(listener).onQueryInventoryFinished(new IabResult(Response.ERROR), null);
    }

    @Test public void shouldNotNotifyListenerIfHelperWasDisposed() throws Exception {
        Inventory inventory = mock(Inventory.class);
        when(iabHelper.queryInventory(false, null, null)).thenReturn(inventory);
        when(iabHelper.isDisposed()).thenReturn(true);
        task.execute(new QueryInventoryTask.Args(false, null, null)).get();
        verifyZeroInteractions(listener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckArgumentsNull() throws Exception {
        task.doInBackground((QueryInventoryTask.Args) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckArgumentsEmpty() throws Exception {
        task.doInBackground(new QueryInventoryTask.Args[0]);
    }
}
