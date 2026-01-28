package com.sk.domain.admin.listener.event;

import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import java.util.List;
import lombok.Getter;

@Getter
public class AXUploadFileEvent {
    private final Integer goodsDataNo;
    private final Boolean isNew;
    private final List<UploadedFile> deletedFiles;

    public AXUploadFileEvent(Integer goodsDataNo, Boolean isNew, List<UploadedFile> deletedFiles) {
        this.goodsDataNo = goodsDataNo;
        this.isNew = isNew;
        this.deletedFiles = deletedFiles;
    }
    public AXUploadFileEvent(Integer goodsDataNo, Boolean isNew) {
        this.goodsDataNo = goodsDataNo;
        this.isNew = isNew;
        this.deletedFiles = null;
    }
}
