package com.moa.moa_be.domain.capture.entity;

import com.moa.moa_be.domain.user.entity.User;
import com.moa.moa_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** 참고함 폴더. 회원 탈퇴 시 DB에서 함께 삭제 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "folders", uniqueConstraints =
        @UniqueConstraint(name = "uk_folders_user_name", columnNames = {"user_id", "folder_name"}))
public class Folder extends BaseTimeEntity {

    public static final int MAX_NAME_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "folder_name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    public Folder(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
