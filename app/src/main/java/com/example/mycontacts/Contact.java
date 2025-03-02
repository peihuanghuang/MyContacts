package com.example.mycontacts;

import com.google.auto.value.AutoValue;
import java.util.List;

/**
 *
 */
@AutoValue
public abstract class Contact {

  public static Builder builder() {
    return new AutoValue_Contact.Builder();
  }
  public abstract String account();
  public abstract String displayName();
  public abstract List<String> phoneNumber();
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setAccount(String account);

    public abstract Builder setDisplayName(String value);

    public abstract Builder setPhoneNumber(List<String> value);

    public abstract Contact build();
  }
}
