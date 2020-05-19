//
//  InfoViewController.swift
//  Demo
//
//  Created by Gijs van Veen on 11/05/2020.
//  Copyright © 2020 Splendo. All rights reserved.
//

import UIKit
import KotlinNativeFramework

class InfoViewController : UITableViewController {
    
    private lazy var viewModel: SharedInfoViewModel = KNArchitectureFramework().createInfoViewModel(parent: self)
    private let disposeBag = ArchitectureDisposeBag()

    private var buttons = [String]()
    private var onSelected: ((KotlinInt) -> KotlinUnit)? = nil
    
    deinit {
        viewModel.clear()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        viewModel.didResume()
        
        viewModel.observeButtons(disposeBag: disposeBag) { (buttons: [String], onSelected: @escaping (KotlinInt) -> KotlinUnit) in
            self.buttons = buttons
            self.onSelected = onSelected
            self.tableView.reloadData()
        }
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        viewModel.didPause()
        disposeBag.dispose()
    }
    
    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return buttons.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: InfoButtonCell.Const.identifier, for: indexPath) as! InfoButtonCell
        cell.label.text = buttons[indexPath.row]
        return cell
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let _ = onSelected?(KotlinInt.init(int: Int32(indexPath.row)))
        tableView.deselectRow(at: indexPath, animated: true)
    }
    
}

class InfoButtonCell : UITableViewCell {
    
    struct Const {
        static let identifier = "InfoButtonCell"
    }
    
    @IBOutlet weak var label: UILabel!
    
}
