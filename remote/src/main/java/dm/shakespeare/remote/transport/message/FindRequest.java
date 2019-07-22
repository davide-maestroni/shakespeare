/*
 * Copyright 2019 Davide Maestroni
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

package dm.shakespeare.remote.transport.message;

import org.jetbrains.annotations.NotNull;

import dm.shakespeare.actor.Actor;
import dm.shakespeare.function.Tester;

/**
 * Created by davide-maestroni on 05/24/2019.
 */
public class FindRequest extends RemoteRequest {

  private static final long serialVersionUID = VERSION;

  private FilterType filterType;
  private String pattern;
  private Tester<? super Actor> tester;

  @NotNull
  public FindResponse buildResponse() {
    return new FindResponse();
  }

  @NotNull
  @Override
  public FindRequest withSenderId(final String senderId) {
    super.withSenderId(senderId);
    return this;
  }

  public FilterType getFilterType() {
    return filterType;
  }

  public void setFilterType(final FilterType filterType) {
    this.filterType = filterType;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(final String pattern) {
    this.pattern = pattern;
  }

  public Tester<? super Actor> getTester() {
    return tester;
  }

  public void setTester(final Tester<? super Actor> tester) {
    this.tester = tester;
  }

  @NotNull
  public FindRequest withFilterType(final FilterType filterType) {
    this.filterType = filterType;
    return this;
  }

  @NotNull
  public FindRequest withPattern(final String pattern) {
    this.pattern = pattern;
    return this;
  }

  @NotNull
  public FindRequest withTester(final Tester<? super Actor> tester) {
    this.tester = tester;
    return this;
  }

  public enum FilterType {
    ALL, ANY, EXACT
  }
}
