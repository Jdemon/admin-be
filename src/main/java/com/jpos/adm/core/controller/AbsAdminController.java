package com.jpos.adm.core.controller;

import com.jpos.adm.core.entity.BaseModel;
import com.jpos.adm.core.exception.CantDeleteException;
import com.jpos.adm.core.exception.NotFoundException;
import com.jpos.adm.core.extension.ClazzUtil;
import com.jpos.adm.core.service.AbsAdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Optional;

public abstract class AbsAdminController<T extends BaseModel> {
    private final AbsAdminService<T> service;
    private final Class<T> clazz;

    public AbsAdminController(AbsAdminService<T> service) {
        this.service = service;
        this.clazz = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    @GetMapping
    public Page<T> find(@RequestParam Map<String, String> params, Pageable pageable) {
        return service.find(params, pageable);
    }

    @GetMapping("{id}")
    public ResponseEntity<T> findById(@PathVariable String id) {
        Optional<T> optT = service.findById(id);
        return optT.map(ResponseEntity::ok).orElseThrow(NotFoundException::new);
    }

    @PostMapping
    public ResponseEntity<T> save(@RequestBody T t) {
        Optional<T> optT = service.save(t);
        return optT.map(ResponseEntity::ok).orElseThrow(NotFoundException::new);
    }

    @PutMapping("{id}")
    public ResponseEntity<T> update(@PathVariable String id, @RequestBody T t) {
        if (service.findById(id).isEmpty()) {
            throw new NotFoundException();
        }
        Optional<T> optT = service.update(id, t);
        return optT.map(ResponseEntity::ok).orElseThrow(NotFoundException::new);
    }

    @PatchMapping("{id}")
    public ResponseEntity<T> patch(@PathVariable String id, @RequestBody T t) {
        if (service.findById(id).isEmpty()) {
            throw new NotFoundException();
        }
        Optional<T> optT = service.patch(id, t);
        return optT.map(ResponseEntity::ok).orElseThrow(NotFoundException::new);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<T> delete(@PathVariable String id) {
        if (service.findById(id).isEmpty()) {
            throw new CantDeleteException();
        }
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("clear/all")
    public ResponseEntity<?> deleteAll() {
        service.deleteAll();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export/excel")
    public ResponseEntity<?> exportExcel(HttpServletResponse response, @RequestParam Map<String, String> params) {
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "filename=" + ClazzUtil.tableName(clazz) + "_" + System.currentTimeMillis() + ".xlsx");
            byte[] byteArray = service.exportExcel(params);
            response.setContentLength(byteArray.length);
            outputStream.write(byteArray);
        } catch (Throwable e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/import/excel")
    public ResponseEntity<?> importExcel(MultipartFile file) throws IOException {
        service.importExcel(file.getBytes());
        return ResponseEntity.ok().build();
    }

    @GetMapping("schema")
    public ResponseEntity<?> getSchema() {
        return ResponseEntity.ok(ClazzUtil.entitySchema(clazz));
    }

}
