// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2020-2022 MariaDB Corporation Ab
package org.mariadb.r2dbc.authentication.standard.ed25519.math;

import java.io.Serializable;

/**
 * An EdDSA finite field. Includes several pre-computed values.
 *
 * @author str4d
 */
public class Field implements Serializable {

  private static final long serialVersionUID = 8746587465875676L;

  public final FieldElement ZERO;
  public final FieldElement ONE;
  public final FieldElement TWO;
  public final FieldElement FOUR;
  public final FieldElement FIVE;
  public final FieldElement EIGHT;

  private final int b;
  private final FieldElement q;
  /** q-2 */
  private final FieldElement qm2;
  /** (q-5) / 8 */
  private final FieldElement qm5d8;

  private final Encoding enc;

  public Field(int b, byte[] q, Encoding enc) {
    this.b = b;
    this.enc = enc;
    this.enc.setField(this);

    this.q = fromByteArray(q);

    // Set up constants
    ZERO = fromByteArray(Constants.ZERO);
    ONE = fromByteArray(Constants.ONE);
    TWO = fromByteArray(Constants.TWO);
    FOUR = fromByteArray(Constants.FOUR);
    FIVE = fromByteArray(Constants.FIVE);
    EIGHT = fromByteArray(Constants.EIGHT);

    // Precompute values
    qm2 = this.q.subtract(TWO);
    qm5d8 = this.q.subtract(FIVE).divide(EIGHT);
  }

  public FieldElement fromByteArray(byte[] x) {
    return enc.decode(x);
  }

  public int getb() {
    return b;
  }

  public FieldElement getQ() {
    return q;
  }

  public FieldElement getQm2() {
    return qm2;
  }

  public FieldElement getQm5d8() {
    return qm5d8;
  }

  public Encoding getEncoding() {
    return enc;
  }

  @Override
  public int hashCode() {
    return q.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Field)) {
      return false;
    }
    Field f = (Field) obj;
    return b == f.b && q.equals(f.q);
  }
}
