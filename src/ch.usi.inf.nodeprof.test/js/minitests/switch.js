/*******************************************************************************
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *******************************************************************************/

let bar = 1; // statement-end (discard)

['42'].forEach(x => {
  switch (x) { // switch discriminant: extra statement-end (discard)
    case 1:
      break;
    default:
      bar--; // statement-end (discard)
    case 1 + bar:
      foo = 2; // statement-end (discard)
      break;
    case '42' + bar:
      'foo' + 'bar';
      break;
} // TODO: switch is not a statement
}); // statement-end (discard)

switch (foo + 1) { // switch discriminant: extra statement-end (discard)
  case 3: // conditional: extra statement-end (discard)
    bar--; // statement-end (discard)
    console.log(bar); // statement-end (discard)
    break;
  default:
    bar--;
    break;
} // TODO: switch is not a statement
