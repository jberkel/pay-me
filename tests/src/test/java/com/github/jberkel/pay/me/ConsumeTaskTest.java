package com.github.jberkel.pay.me;

import com.github.jberkel.pay.me.listener.OnConsumeFinishedListener;
import com.github.jberkel.pay.me.listener.OnConsumeMultiFinishedListener;
import com.github.jberkel.pay.me.model.Purchase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.github.jberkel.pay.me.Response.ERROR;
import static com.github.jberkel.pay.me.Response.OK;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
public class ConsumeTaskTest {
    @Mock
    OnConsumeFinishedListener consumeFinishedListener;
    @Mock
    OnConsumeMultiFinishedListener consumeMultiFinishedListener;
    @Mock
    IabHelper iabHelper;
    ConsumeTask task;

    @Before public void before() throws Exception {
        initMocks(this);
        task = new ConsumeTask(iabHelper, consumeFinishedListener, consumeMultiFinishedListener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyArgumentsEmpty() throws Exception {
        task.doInBackground();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyArgumentsNull() throws Exception {
        task.doInBackground((Purchase[]) null);
    }

    @Test public void shouldConsumeItem() throws Exception {
        Purchase p = mock(Purchase.class);

        List<IabResult> results = task.execute(p).get();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResponse()).isEqualTo(OK);

        verify(iabHelper).consume(p);
        verify(consumeFinishedListener).onConsumeFinished(p, new IabResult(OK));
        verify(consumeMultiFinishedListener).onConsumeMultiFinished(anyListOf(Purchase.class), anyListOf(IabResult.class));
    }

    @Test public void shouldConsumeItemFailureCase() throws Exception {
        Purchase p = mock(Purchase.class);

        doThrow(new IabException(Response.ERROR, "error")).when(iabHelper).consume(p);

        List<IabResult> results = task.execute(p).get();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getResponse()).isEqualTo(ERROR);

        verify(consumeFinishedListener).onConsumeFinished(p, new IabResult(ERROR));
        verify(consumeMultiFinishedListener).onConsumeMultiFinished(anyListOf(Purchase.class), anyListOf(IabResult.class));
    }

    @Test public void shouldConsumeMultipleItems() throws Exception {
        Purchase p1 = mock(Purchase.class);
        Purchase p2 = mock(Purchase.class);

        List<IabResult> results = task.execute(p1, p2).get();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getResponse()).isEqualTo(OK);

        verify(iabHelper, times(2)).consume(any(Purchase.class));

        verify(consumeFinishedListener).onConsumeFinished(p1, new IabResult(OK));
        verify(consumeMultiFinishedListener).onConsumeMultiFinished(anyListOf(Purchase.class), anyListOf(IabResult.class));
    }

    @Test public void shouldNotNotifyListenerIfHelperWasDisposed() throws Exception {
        Purchase p = mock(Purchase.class);

        when(iabHelper.isDisposed()).thenReturn(true);
        task.execute(p).get();

        verifyZeroInteractions(consumeFinishedListener);
        verifyZeroInteractions(consumeMultiFinishedListener);
    }
}
