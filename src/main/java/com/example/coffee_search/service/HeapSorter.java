package com.example.coffee_search.service;

import com.example.coffee_search.model.SearchResult;
import java.util.ArrayList;
import java.util.List;

public class HeapSorter {
    private ArrayList<SearchResult> heap;

    public HeapSorter() {
        this.heap = new ArrayList<>();
    }

    public void insert(SearchResult result) {
        heap.add(result);
        heapifyUp(heap.size() - 1);
    }

    public SearchResult extractMax() {
        if (heap.isEmpty()) return null;

        SearchResult max = heap.get(0);
        SearchResult last = heap.remove(heap.size() - 1);

        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return max;
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    private void heapifyUp(int index) {
        int parentIndex = (index - 1) / 2;
        if (index > 0 && heap.get(index).getScore() > heap.get(parentIndex).getScore()) {
            swap(index, parentIndex);
            heapifyUp(parentIndex);
        }
    }

    private void heapifyDown(int index) {
        int largest = index;
        int leftChild = 2 * index + 1;
        int rightChild = 2 * index + 2;

        if (leftChild < heap.size() && heap.get(leftChild).getScore() > heap.get(largest).getScore()) {
            largest = leftChild;
        }

        if (rightChild < heap.size() && heap.get(rightChild).getScore() > heap.get(largest).getScore()) {
            largest = rightChild;
        }

        if (largest != index) {
            swap(index, largest);
            heapifyDown(largest);
        }
    }

    private void swap(int i, int j) {
        SearchResult temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
    
    // 一次取得所有排序好的結果
    public List<SearchResult> getSortedList() {
        List<SearchResult> sortedList = new ArrayList<>();
        // 為了不破壞原本的 heap 結構，我們複製一份來操作 extraction
        ArrayList<SearchResult> tempHeap = new ArrayList<>(this.heap);
        HeapSorter tempSorter = new HeapSorter();
        tempSorter.heap = tempHeap;
        
        while (!tempSorter.isEmpty()) {
            sortedList.add(tempSorter.extractMax());
        }
        return sortedList;
    }
}