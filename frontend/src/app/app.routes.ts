import { Routes } from '@angular/router';
import { BatchListPage } from './pages/batch-list.page';
import { BatchDetailPage } from './pages/batch-detail.page';
import { IssueListPage } from './pages/issue-list.page';
import { IssueDetailPage } from './pages/issue-detail.page';
import { ComparePage } from './pages/compare.page';
import { ConsistencyPage } from './pages/consistency.page';

export const routes: Routes = [
  { path: '', redirectTo: 'batches', pathMatch: 'full' },
  { path: 'batches', component: BatchListPage },
  { path: 'batches/:code', component: BatchDetailPage },
  { path: 'issues', component: IssueListPage },
  { path: 'issues/:code', component: IssueDetailPage },
  { path: 'compare', component: ComparePage },
  { path: 'consistency', component: ConsistencyPage },
  { path: '**', redirectTo: 'batches' },
];
