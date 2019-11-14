//
//  LoadingViewController.swift
//  Demo
//
//  Created by Grigory Avdyushin on 31/10/2019.
//  Copyright © 2019 Splendo. All rights reserved.
//

import UIKit
import KotlinNativeFramework

class LoadingViewController: UITableViewController {

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        if indexPath.row == 0 {
            showLoadingIndicator()
        }
    }

    fileprivate func showLoadingIndicator() {
        let indicator = KotlinNativeFrameworkKt.activityIndicator(viewController: self)
        indicator.show(animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            indicator.dismiss(animated: true)
        }
    }
}
