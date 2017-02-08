/*
Copyright [2017] [DCS <Info-dcs@diehl.com>]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.diehl.dcs.kalinka.pub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = { JmsAutoConfiguration.class, HealthIndicatorAutoConfiguration.class })
public class Main {

	public static void main(final String[] args) {

		SpringApplication.run(Main.class, args);
	}
}
