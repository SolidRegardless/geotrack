import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/api.service';
import { Asset, AssetType, AssetStatus } from '../../../core/models';

@Component({
  selector: 'app-asset-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './asset-list.component.html',
  styleUrl: './asset-list.component.scss'
})
export class AssetListComponent implements OnInit {

  assets: Asset[] = [];
  filteredAssets: Asset[] = [];

  typeFilter: AssetType | '' = '';
  statusFilter: AssetStatus | '' = '';

  assetTypes: AssetType[] = ['VEHICLE', 'DRONE', 'VESSEL', 'PERSONNEL', 'AIRCRAFT', 'SENSOR'];
  assetStatuses: AssetStatus[] = ['ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DECOMMISSIONED'];

  showCreateForm = false;
  newAssetName = '';
  newAssetType: AssetType = 'VEHICLE';

  selectedAsset: Asset | null = null;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadAssets();
  }

  loadAssets(): void {
    const type = this.typeFilter || undefined;
    const status = this.statusFilter || undefined;
    this.api.getAssets(type, status).subscribe({
      next: (assets) => {
        this.assets = assets;
        this.applyFilters();
      },
      error: (err) => console.error('Failed to load assets:', err)
    });
  }

  applyFilters(): void {
    this.filteredAssets = this.assets.filter(a => {
      if (this.typeFilter && a.type !== this.typeFilter) return false;
      if (this.statusFilter && a.status !== this.statusFilter) return false;
      return true;
    });
  }

  onFilterChange(): void {
    this.loadAssets();
  }

  createAsset(): void {
    if (!this.newAssetName.trim()) return;
    this.api.createAsset({ name: this.newAssetName.trim(), type: this.newAssetType }).subscribe({
      next: () => {
        this.newAssetName = '';
        this.showCreateForm = false;
        this.loadAssets();
      },
      error: (err) => console.error('Failed to create asset:', err)
    });
  }

  deleteAsset(event: Event, asset: Asset): void {
    event.stopPropagation();
    if (!confirm(`Delete "${asset.name}"?`)) return;
    this.api.deleteAsset(asset.id).subscribe({
      next: () => this.loadAssets(),
      error: (err) => console.error('Failed to delete asset:', err)
    });
  }

  selectAsset(asset: Asset): void {
    this.selectedAsset = this.selectedAsset?.id === asset.id ? null : asset;
  }
}
