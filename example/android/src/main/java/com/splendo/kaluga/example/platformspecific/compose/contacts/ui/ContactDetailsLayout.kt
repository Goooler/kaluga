/*
 Copyright 2021 Splendo Consulting B.V. The Netherlands

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

package com.splendo.kaluga.example.platformspecific.compose.contacts.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.splendo.kaluga.architecture.compose.navigation.HardwareBackButtonNavigation
import com.splendo.kaluga.architecture.compose.viewModel.ViewModelComposable
import com.splendo.kaluga.architecture.compose.viewModel.store
import com.splendo.kaluga.architecture.navigation.Navigator
import com.splendo.kaluga.example.shared.platformspecific.compose.contacts.model.ContactDetails
import com.splendo.kaluga.example.shared.platformspecific.compose.contacts.viewModel.ContactsNavigation
import com.splendo.kaluga.example.shared.platformspecific.compose.contacts.viewModel.ContactDetailsViewModel

@Composable
fun ContactDetailsLayout(contactDetails: ContactDetails, navigator: Navigator<ContactsNavigation<*>>) {
    val viewModel = store {
        remember {
            ContactDetailsViewModel(contactDetails, navigator)
        }
    }
    ViewModelComposable(viewModel) {
        HardwareBackButtonNavigation(::back)
        Column {
            Text(text = contactDetails.name)
            Text(text = contactDetails.email)

            Button(onClick = ::sendEmail) {
                Text(text = sendEmailButtonText)
            }
        }
    }
}