package com.sk.domain.admin.listener.event;

import com.sk.domain.shared.entity.master.tcm.UploadedFile;
import java.util.List;

public record AXUploadFileEvent(Integer goodsDataNo, Boolean isNew,
                                List<UploadedFile> deletedFiles) {

  public AXUploadFileEvent(Integer goodsDataNo, Boolean isNew) {
    this(goodsDataNo, isNew, null);
  }
}
