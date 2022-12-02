// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2020-2022 MariaDB Corporation Ab

package org.mariadb.r2dbc.client;

import io.netty.util.ReferenceCountUtil;
import java.util.concurrent.atomic.AtomicLong;
import org.mariadb.r2dbc.message.ServerMessage;
import reactor.core.publisher.FluxSink;

public class Exchange {

  private final FluxSink<ServerMessage> sink;
  private final DecoderState initialState;
  private final String sql;
  private final AtomicLong demand = new AtomicLong();

  public Exchange(FluxSink<ServerMessage> sink, DecoderState initialState) {
    this.sink = sink;
    this.initialState = initialState;
    this.sql = null;
  }

  public Exchange(FluxSink<ServerMessage> sink, DecoderState initialState, String sql) {
    this.sink = sink;
    this.initialState = initialState;
    this.sql = sql;
  }

  public FluxSink<ServerMessage> getSink() {
    return sink;
  }

  public DecoderState getInitialState() {
    return initialState;
  }

  public String getSql() {
    return sql;
  }

  public boolean hasDemand() {
    return demand.get() > 0;
  }

  public boolean isCancelled() {
    return sink.isCancelled();
  }

  public void onError(Throwable throwable) {
    if (!this.sink.isCancelled()) {
      this.sink.error(throwable);
    }
  }

  /**
   * Emit server message.
   *
   * @param srvMsg message to emit
   * @return true if ending message
   */
  public boolean emit(ServerMessage srvMsg) {
    if (this.sink.isCancelled()) {
      ReferenceCountUtil.release(srvMsg);
      return srvMsg.ending();
    }

    demand.decrementAndGet();
    try {
      this.sink.next(srvMsg);
      if (srvMsg.ending()) {
        if (!this.sink.isCancelled()) {
          this.sink.complete();
        }
        return true;
      }
      return false;
    } catch (Throwable t) {
      ReferenceCountUtil.release(srvMsg);
      return false;
    }
  }

  public void incrementDemand(long n) {
    demand.addAndGet(n);
  }
}
