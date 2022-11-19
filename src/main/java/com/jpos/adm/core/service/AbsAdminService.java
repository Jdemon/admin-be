package com.jpos.adm.core.service;

import com.jpos.adm.core.entity.BaseModel;
import com.jpos.adm.core.excel.ExcelExporter;
import com.jpos.adm.core.excel.ExcelImporter;
import com.jpos.adm.core.exception.FilterException;
import com.jpos.adm.core.extension.ClazzUtil;
import com.jpos.adm.core.extension.ModelConstant;
import com.jpos.adm.core.repository.IRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Id;
import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AbsAdminService<T extends BaseModel> {

    protected final IRepository<T> repository;
    protected Class<T> clazz;
    protected static final String QUERY = "q";
    protected static final String SORT = "sort";
    protected static final String GREATER_THAN_EQUAL = "_gte";
    protected static final String LESS_THAN_EQUAL = "_lte";
    protected static final String NOT_EQUAL = "_ne";
    protected static final String LIKE = "_like";
    private static final Set<Class<?>> primitiveNumbers = Stream
            .of(int.class, long.class, float.class,
                    double.class, byte.class, short.class)
            .collect(Collectors.toSet());

    public AbsAdminService(IRepository<T> repository) {
        this.repository = repository;
        this.clazz = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0];
    }

    @Transactional(readOnly = true ,propagation = Propagation.REQUIRES_NEW)
    public Page<T> find(Map<String, String> params, Pageable pageable) {
        return repository.findAll(coreSpecification(params), pageable);
    }

    @Transactional(readOnly = true ,propagation = Propagation.REQUIRES_NEW)
    @Cacheable(
            cacheNames = "admin",
            unless = "#result == null",
            cacheManager = "cacheManager",
            keyGenerator = "keyGenerator"
    )
    public Optional<T> findById(String id) {
        return repository.findById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<T> save(T t) {
        t.setId(null);
        return Optional.of(repository.save(t));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(
            cacheNames = "admin",
            cacheManager = "cacheManager",
            keyGenerator = "keyGenerator"
    )
    public Optional<T> update(String id, T t) {
        Optional<T> original = repository.findById(id);
        t.setId(id);
        original.ifPresent(value ->
                {
                    t.setCreatedAt(value.getCreatedAt());
                    t.setCreatedBy(value.getCreatedBy());
                }
        );
        return Optional.of(repository.save(t));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(
            cacheNames = "admin",
            cacheManager = "cacheManager",
            keyGenerator = "keyGenerator"
    )
    public Optional<T> patch(String id, T t) {
        Optional<T> opt = repository.findById(id);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        T original = opt.get();
        copyPropertiesIgnoreNull(t, original);
        return update(id, original);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(
            cacheNames = "admin",
            cacheManager = "cacheManager",
            keyGenerator = "keyGenerator"
    )
    public void delete(String id) {
        repository.deleteById(id);
    }

    protected Specification<T> coreSpecification(Map<String, String> params) {
        try {
            return (root, query, cb) -> {
                boolean isFullText = params.containsKey(QUERY);
                Field[] fields = ClazzUtil.getAllFields(this.clazz);
                if (isFullText) {
                    if (!params.containsKey(SORT)) {
                        query.orderBy(cb.asc(root.get(ModelConstant.ID)));
                    }
                    String q = params.get(QUERY).toLowerCase();
                    List<Predicate> predicates = fullTextSearch(fields, q, root, cb);
                    predicates = customFullTextSearch(predicates, q, root, cb);
                    return cb.or(predicates.toArray(new Predicate[0]));
                }
                return searchByFields(fields, params, root, query, cb);
            };
        } catch (Exception e) {
            throw new FilterException(e);
        }
    }

    protected List<Predicate> customFullTextSearch(List<Predicate> predicates, String query, Root<T> root, CriteriaBuilder cb) {
        return predicates;
    }

    public List<Predicate> fullTextSearch(Field[] fields, String query, Root<T> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        for (Field field : fields) {
            predicates.add(cb.like(cb.lower(root.get(field.getName()).as(String.class)),
                    "%" + query + "%"));
        }
        return predicates;
    }

    protected Predicate searchByFields(Field[] fields, Map<String, String> params, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        for (Field field : fields) {
            if (!params.containsKey(SORT) && field.isAnnotationPresent(Id.class)) {
                query.orderBy(cb.asc(root.get(field.getName())));
            }

            if (params.containsKey(field.getName())) {
                List<Predicate> fieldPredicate = new ArrayList<>();
                String[] values = StringUtils.split(params.get(field.getName()), ',');
                for (String val : values) {
                    fieldPredicate.add(cb.equal(root.get(field.getName()).as(String.class),
                            StringUtils.trim(val)));
                }
                predicates.add(cb.or(fieldPredicate.toArray(new Predicate[0])));
            }
            String gteVal = params.get(field.getName() + GREATER_THAN_EQUAL);
            String lteVal = params.get(field.getName() + LESS_THAN_EQUAL);
            String neVal = params.get(field.getName() + NOT_EQUAL);
            String likeVal = params.get(field.getName() + LIKE);

            if (StringUtils.isNotBlank(neVal)) {
                String[] values = StringUtils.split(neVal, ',');
                for (String val : values) {
                    predicates.add(cb.notEqual(root.get(field.getName()).as(String.class),
                            val));
                }
            }


            if (StringUtils.isNotBlank(likeVal)) {
                predicates.add(cb.like(cb.lower(root.get(field.getName()).as(String.class)),
                        "%" + likeVal + "%"));
            }

            if (isNumericType(field.getType()) && NumberUtils.isCreatable(lteVal)) {
                try {
                    if (StringUtils.isNotBlank(lteVal) && NumberUtils.isCreatable(lteVal)) {
                        predicates.add(cb.le(root.get(field.getName()), NumberFormat.getInstance().parse(lteVal)));
                    }
                    if (StringUtils.isNotBlank(gteVal) && NumberUtils.isCreatable(gteVal)) {
                        predicates.add(cb.ge(root.get(field.getName()), NumberFormat.getInstance().parse(gteVal)));
                    }
                } catch (ParseException e) {
                    log.warn(ExceptionUtils.getStackTrace(e), e);
                }
            }
            if (field.getType().isAssignableFrom(LocalDateTime.class)) {
                if (StringUtils.isNotBlank(lteVal)) {
                    LocalDateTime lteDateTime = LocalDateTime.parse(lteVal, DateTimeFormatter.ofPattern(ClazzUtil.DATETIME_FORMAT));
                    predicates.add(cb.lessThanOrEqualTo(root.get(field.getName()),
                            lteDateTime));
                }
                if (StringUtils.isNotBlank(gteVal)) {
                    LocalDateTime gteDateTime = LocalDateTime.parse(gteVal, DateTimeFormatter.ofPattern(ClazzUtil.DATETIME_FORMAT));
                    predicates.add(cb.greaterThanOrEqualTo(root.get(field.getName()),
                            gteDateTime));
                }
            }

            if (field.getType().isAssignableFrom(LocalDate.class)) {
                if (StringUtils.isNotBlank(lteVal)) {
                    LocalDate lteDateTime = LocalDate.parse(lteVal, DateTimeFormatter.ofPattern(ClazzUtil.DATE_FORMAT));
                    predicates.add(cb.lessThanOrEqualTo(root.get(field.getName()),
                            lteDateTime));
                }
                if (StringUtils.isNotBlank(gteVal)) {
                    LocalDate gteDateTime = LocalDate.parse(gteVal, DateTimeFormatter.ofPattern(ClazzUtil.DATE_FORMAT));
                    predicates.add(cb.greaterThanOrEqualTo(root.get(field.getName()),
                            gteDateTime));
                }
            }
        }
        if (!predicates.isEmpty()) {
            return cb.and(predicates.toArray(new Predicate[0]));
        } else {
            return null;
        }
    }

    private static boolean isNumericType(Class<?> cls) {
        if (cls.isPrimitive()) {
            return primitiveNumbers.contains(cls);
        } else {
            return Number.class.isAssignableFrom(cls);
        }
    }

    public String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) emptyNames.add(pd.getName());
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    // then use Spring BeanUtils to copy and ignore null using our function
    public void copyPropertiesIgnoreNull(Object src, Object target) {
        BeanUtils.copyProperties(src, target, getNullPropertyNames(src));
    }

    public byte[] exportExcel(Map<String, String> params) {
        List<T> allData = repository.findAll(coreSpecification(params));
        return new ExcelExporter<T>(allData, clazz).exportExcel();
    }

    @CacheEvict(
            cacheNames = "admin",
            cacheManager = "cacheManager",
            allEntries = true
    )
    public void importExcel(byte[] fileData) {
        saveList(new ExcelImporter<T>(clazz).importExcel(fileData));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(
            cacheNames = "admin",
            cacheManager = "cacheManager",
            allEntries = true
    )
    public void deleteAll() {
        repository.deleteAll();
    }

    private void saveList(List<T> listOfT) {
        for (T t : listOfT) {
            String id = t.getId();
            if (id != null) {
                update(id, t);
            } else {
                save(t);
            }
        }
    }
}
