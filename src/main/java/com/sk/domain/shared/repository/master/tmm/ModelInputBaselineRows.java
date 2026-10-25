package com.sk.domain.shared.repository.master.tmm;

import com.sk.domain.mi.service.fundamentalmodel.input.InputDraftException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.*;

/** 공유 원본의 개인 행 ID. 파일 내용 해시나 접근 권한으로 사용하지 않는다. */
public final class ModelInputBaselineRows {
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private ModelInputBaselineRows() {}

    public static String id(String draft, String baseline, long table, int original) {
        return new Ids(draft, baseline).id(table, original);
    }

    /** 한 행 목록의 순차 처리에만 사용한다. 요청/스레드 간 공유하지 않는다. */
    static final class Ids {
        private final String draft;
        private final String baseline;
        private byte[] prefix;
        private MessageDigest sha;

        Ids(String draft, String baseline) {
            this.draft = draft;
            this.baseline = baseline;
        }

        String id(long table, int original) {
            if (table <= 0 || original < 1 || original > 1048576) throw InputDraftException.internal();
            // 빈 목록에서는 UUID를 검사하지 않던 동작과 행 번호 검사 순서를 유지한다.
            if (sha == null) initialize();
            sha.update(prefix);
            byte[] bytes = sha.digest((table + "/" + original).getBytes(StandardCharsets.UTF_8));
            bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x50);
            bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);
            ByteBuffer value = ByteBuffer.wrap(bytes);
            return new UUID(value.getLong(), value.getLong()).toString();
        }

        private void initialize() {
            UUID namespace = uuid(draft);
            uuid(baseline);
            byte[] name = ("mi-input-row/v1/" + baseline + "/").getBytes(StandardCharsets.UTF_8);
            prefix = ByteBuffer.allocate(16 + name.length).putLong(namespace.getMostSignificantBits())
                .putLong(namespace.getLeastSignificantBits()).put(name).array();
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException ex) {
                throw InputDraftException.internal();
            }
        }
    }

    public record Snapshot(List<RowIdentity> identities,List<RowPosition> positions) {
        public Snapshot { identities=List.copyOf(identities);positions=List.copyOf(positions); }
    }

    /** 물리 원본 override를 중복 합치지 않고, 누락 구간을 가상 원본으로 숨기지 않는다. */
    public static Snapshot merge(Draft draft,List<RowIdentity> originals,List<RowIdentity> stored,List<RowPosition> positions) {
        require(draft.baselineId()!=null&&!draft.uploaded());
        Map<String,RowIdentity> logical=new LinkedHashMap<>();
        Ids ids=new Ids(draft.id(),draft.baselineId());
        for(RowIdentity row:originals) {
            require(row.original()!=null&&row.operation()==null&&row.client()==null&&row.baseline()==0
                &&row.id().equals(ids.id(row.table(),row.original())));
            require(logical.putIfAbsent(row.id(),row)==null);
        }
        Map<String,RowIdentity> physical=new LinkedHashMap<>();
        for(RowIdentity row:stored) {
            require(row.baseline()==0&&physical.putIfAbsent(row.id(),row)==null);
            if(row.original()!=null)require(row.equals(logical.get(row.id())));
            else require(row.operation()!=null&&row.client()!=null&&logical.putIfAbsent(row.id(),row)==null);
        }
        Map<String,List<RowPosition>> grouped=new HashMap<>();
        for(RowPosition p:positions) {
            RowIdentity row=physical.get(p.id());
            require(row!=null&&row.table()==p.table()&&p.begin()>=0&&p.begin()<=draft.revision()
                &&(p.end()==null||p.end()>p.begin()&&p.end()<=draft.revision())
                &&(p.row()==null||p.row()>=1&&p.row()<=1048576));
            grouped.computeIfAbsent(p.id(),ignored->new ArrayList<>()).add(p);
        }
        List<RowPosition> all=new ArrayList<>();
        Set<String> occupied=new HashSet<>();
        for(RowIdentity row:logical.values()) {
            List<RowPosition> chain=grouped.get(row.id());
            if(!physical.containsKey(row.id()))chain=List.of(new RowPosition(row.table(),row.id(),0,null,row.original()));
            else {
                require(chain!=null&&!chain.isEmpty());
                chain.sort(java.util.Comparator.comparingLong(RowPosition::begin));
                RowPosition first=chain.get(0);
                if(row.original()!=null)require(chain.size()>=2&&first.begin()==0&&row.original().equals(first.row()));
                else require(first.begin()>0);
                for(int i=0;i<chain.size();i++) {
                    Long expected=i+1<chain.size()?chain.get(i+1).begin():null;
                    require(java.util.Objects.equals(chain.get(i).end(),expected));
                }
            }
            RowPosition current=chain.get(chain.size()-1);
            require(current.end()==null);
            if(current.row()!=null)require(occupied.add(current.table()+":"+current.row()));
            all.addAll(chain);
        }
        return new Snapshot(new ArrayList<>(logical.values()),all);
    }
    private static void require(boolean valid) { if(!valid)throw InputDraftException.internal(); }

    private static UUID uuid(String value) {
        if (value == null || !UUID_PATTERN.matcher(value).matches())
            throw InputDraftException.internal();
        return UUID.fromString(value);
    }
}
