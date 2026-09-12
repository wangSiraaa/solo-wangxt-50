import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="topbar">
      <span class="logo">🔧 试模对照工作台</span>
      <nav>
        <a routerLink="/batches" routerLinkActive="active">批次工作台</a>
        <a routerLink="/issues" routerLinkActive="active">问题台账</a>
        <a routerLink="/compare" routerLinkActive="active">两轮对照</a>
        <a routerLink="/consistency" routerLinkActive="active">一致性检查</a>
      </nav>
    </div>
    <router-outlet></router-outlet>
  `,
})
export class AppComponent {}
