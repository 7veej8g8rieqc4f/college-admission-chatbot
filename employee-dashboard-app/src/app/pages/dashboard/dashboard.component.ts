import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  companyTitle = 'Acme Employee Dashboard';

  employee = {
    name: 'Aarav Sharma',
    role: 'Senior Angular Developer',
    department: 'Engineering'
  };
}
