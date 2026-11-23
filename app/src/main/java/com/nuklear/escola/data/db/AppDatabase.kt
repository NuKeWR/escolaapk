package com.nuklear.escola.data.db

import android.content.Context
import androidx.room.*
import com.nuklear.escola.data.model.*

@Dao
interface AlunoDao {
    @Query("SELECT * FROM alunos ORDER BY nome")
    suspend fun getAll(): List<Aluno>

    @Query("SELECT * FROM alunos WHERE ativo = 1 ORDER BY nome")
    suspend fun getAtivos(): List<Aluno>

    @Query("SELECT * FROM alunos WHERE ativo = 0 ORDER BY nome")
    suspend fun getInativos(): List<Aluno>

    @Query("SELECT * FROM alunos WHERE ativo = 1 AND horario = :horario ORDER BY nome")
    suspend fun getAtivosByHorario(horario: String): List<Aluno>

    @Query("SELECT * FROM alunos WHERE id = :id")
    suspend fun getById(id: Long): Aluno?

    @Query("SELECT * FROM alunos WHERE ativo = 1 AND nome LIKE '%' || :nome || '%' ORDER BY nome")
    suspend fun buscar(nome: String): List<Aluno>

    @Query("SELECT DISTINCT horario FROM alunos WHERE ativo = 1 AND horario != '' ORDER BY horario")
    suspend fun getHorarios(): List<String>

    @Query("SELECT COUNT(*) FROM alunos WHERE ativo = 1")
    suspend fun countAtivos(): Int

    @Query("SELECT COUNT(*) FROM alunos WHERE ativo = 1 AND turma = :turma")
    suspend fun countByTurma(turma: String): Int

    @Query("SELECT COALESCE(SUM(valor_mensalidade), 0) FROM alunos WHERE ativo = 1")
    suspend fun receitaPrevista(): Double

    @Query("SELECT * FROM alunos WHERE ativo = 1 AND data_nascimento != '' AND substr(data_nascimento, 6, 2) = :mes ORDER BY substr(data_nascimento, 9, 2)")
    suspend fun aniversariantes(mes: String): List<Aluno>

    @Query("SELECT horario, COUNT(*) as total FROM alunos WHERE ativo = 1 AND horario != '' GROUP BY horario ORDER BY horario")
    suspend fun countByHorario(): List<HorarioCount>

    @Query("DELETE FROM alunos")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(aluno: Aluno): Long

    @Update
    suspend fun update(aluno: Aluno)

    @Query("UPDATE alunos SET ativo = :ativo WHERE id = :id")
    suspend fun setAtivo(id: Long, ativo: Int)

    @Query("UPDATE alunos SET faixa_atual = :faixa WHERE id = :id")
    suspend fun updateFaixa(id: Long, faixa: String)

    @Query("DELETE FROM alunos WHERE id = :id")
    suspend fun delete(id: Long)
}

data class HorarioCount(val horario: String, val total: Int)

@Dao
interface PagamentoDao {
    @Query("SELECT * FROM pagamentos WHERE aluno_id = :alunoId ORDER BY data_pagamento DESC")
    suspend fun getByAluno(alunoId: Long): List<Pagamento>

    @Query("SELECT competencia FROM pagamentos WHERE aluno_id = :alunoId ORDER BY competencia DESC LIMIT 1")
    suspend fun ultimaCompetencia(alunoId: Long): String?

    @Query("SELECT COALESCE(SUM(valor_pago), 0) FROM pagamentos WHERE competencia = :comp")
    suspend fun recebidoMes(comp: String): Double

    @Query("SELECT competencia, COALESCE(SUM(valor_pago), 0) as total FROM pagamentos GROUP BY competencia ORDER BY competencia DESC LIMIT 12")
    suspend fun receitaMensal(): List<ReceitaMensal>

    @Query("SELECT a.id as alunoId, a.nome, COALESCE(SUM(p.valor_pago), 0) as total FROM alunos a LEFT JOIN pagamentos p ON a.id = p.aluno_id WHERE a.ativo = 1 GROUP BY a.id ORDER BY total DESC")
    suspend fun totalPorAluno(): List<TotalPorAluno>

    @Query("DELETE FROM pagamentos")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(pagamento: Pagamento): Long

    @Update
    suspend fun update(pagamento: Pagamento)

    @Query("DELETE FROM pagamentos WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pagamentos WHERE aluno_id = :alunoId")
    suspend fun deleteByAluno(alunoId: Long)

    @Query("SELECT * FROM pagamentos WHERE id = :id")
    suspend fun getById(id: Long): Pagamento?
}

data class ReceitaMensal(val competencia: String, val total: Double)
data class TotalPorAluno(val alunoId: Long, val nome: String, val total: Double)

@Dao
interface GraduacaoDao {
    @Query("SELECT * FROM graduacoes WHERE aluno_id = :alunoId ORDER BY data_graduacao DESC")
    suspend fun getByAluno(alunoId: Long): List<Graduacao>

    @Query("DELETE FROM graduacoes")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(graduacao: Graduacao): Long

    @Query("DELETE FROM graduacoes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM graduacoes WHERE aluno_id = :alunoId")
    suspend fun deleteByAluno(alunoId: Long)
}

@Dao
interface PresencaDao {
    @Query("SELECT aluno FROM presencas WHERE data = :data")
    suspend fun getPresentes(data: String): List<String>

    @Query("SELECT COUNT(*) FROM presencas WHERE data = :data")
    suspend fun countPresentes(data: String): Int

    @Query("SELECT data FROM presencas WHERE aluno = :aluno ORDER BY data DESC")
    suspend fun getByAluno(aluno: String): List<String>

    @Query("SELECT aluno, COUNT(*) as total FROM presencas GROUP BY aluno ORDER BY total DESC")
    suspend fun ranking(): List<RankingItem>

    @Query("SELECT COUNT(DISTINCT aluno) FROM presencas WHERE data LIKE :mesPrefix || '%'")
    suspend fun countMes(mesPrefix: String): Int

    @Query("DELETE FROM presencas")
    suspend fun deleteAll()

    @Query("SELECT * FROM presencas ORDER BY data DESC")
    suspend fun getAll(): List<Presenca>

    @Insert
    suspend fun insert(presenca: Presenca)

    @Query("DELETE FROM presencas WHERE data = :data AND aluno = :aluno")
    suspend fun delete(data: String, aluno: String)

    @Query("DELETE FROM presencas WHERE data = :data")
    suspend fun deleteByData(data: String)

    @Query("DELETE FROM presencas WHERE data = :data AND aluno IN (SELECT nome FROM alunos WHERE horario = :horario)")
    suspend fun deleteByDataAndHorario(data: String, horario: String)

    @Query("DELETE FROM presencas WHERE aluno = :aluno")
    suspend fun deleteByAluno(aluno: String)
}

data class RankingItem(val aluno: String, val total: Int)

@Dao
interface FaceEmbeddingDao {
    @Query("SELECT * FROM face_embeddings WHERE aluno_id = :alunoId")
    suspend fun getByAluno(alunoId: Long): List<FaceEmbedding>

    @Query("SELECT * FROM face_embeddings")
    suspend fun getAll(): List<FaceEmbedding>

    @Query("SELECT COUNT(*) FROM face_embeddings WHERE aluno_id = :alunoId")
    suspend fun countByAluno(alunoId: Long): Int

    @Query("DELETE FROM face_embeddings")
    suspend fun deleteAll()

    @Insert
    suspend fun insert(embedding: FaceEmbedding): Long

    @Query("DELETE FROM face_embeddings WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM face_embeddings WHERE aluno_id = :alunoId")
    suspend fun deleteByAluno(alunoId: Long)
}

@Database(
    entities = [Aluno::class, Pagamento::class, Graduacao::class, Presenca::class, FaceEmbedding::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alunoDao(): AlunoDao
    abstract fun pagamentoDao(): PagamentoDao
    abstract fun graduacaoDao(): GraduacaoDao
    abstract fun presencaDao(): PresencaDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "escola_nuklear.db"
                ).build().also { INSTANCE = it }
                // IMPORTANTE: não usar fallbackToDestructiveMigration().
                // Se o banco mudar (nova coluna/tabela), suba "version" acima e
                // crie uma Migration com .addMigrations(...) para NÃO apagar os dados.
            }
        }
    }
}
